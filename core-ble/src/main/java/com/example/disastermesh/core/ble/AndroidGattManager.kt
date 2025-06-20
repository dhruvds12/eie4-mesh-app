package com.example.disastermesh.core.ble

import android.Manifest
import android.bluetooth.*
import android.bluetooth.BluetoothStatusCodes.SUCCESS
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AndroidGattMgr"

//
private val CCCD_UUID: UUID =
    UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")    // standard CCCD

@Singleton
class AndroidGattManager @Inject constructor(
    @ApplicationContext private val ctx: Context
) : GattManager {

    private var gatt: BluetoothGatt? = null
    private var rxChar: BluetoothGattCharacteristic? = null
    private var cccd: BluetoothGattDescriptor? = null        // remember which one we wrote
    private val _events = MutableStateFlow<GattConnectionEvent?>(null)

    private val incoming = MutableSharedFlow<ByteArray>(
        replay = 64,
        extraBufferCapacity = 64,              // hold up to 64 packets
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private data class Pending(val bytes: ByteArray, val ack: CompletableDeferred<Unit>)

    // TODO: Unlimited size queue will negatively impact RAM usage if there is a high volume of messages
    private var outQ = Channel<Pending>(Channel.UNLIMITED)

    /** launched when the first connect() succeeds, cancelled on disconnect() */
    private var writerJob: Job? = null
    private var currentAck: CompletableDeferred<Unit>? = null

    private var connectFlowScope: Job? = null

    /* --------------------------------------------------------------------- */
    /*  connection                                                           */
    /* --------------------------------------------------------------------- */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun connect(address: String): Flow<GattConnectionEvent> = callbackFlow {

        /*  If we were still connected or mid-flow from a previous session,
    shut everything down before starting a new one.               */
        connectFlowScope?.cancel()           // cancel previous collector if any
        cleanup()                            // helper → closes gatt, jobs, channels
        connectFlowScope = this.launch { /* keeps reference so we can cancel later */ }

        val device = ctx.getSystemService(BluetoothManager::class.java)
            ?.adapter?.getRemoteDevice(address)
            ?: run { trySend(GattConnectionEvent.Error("Device not found")); close(); return@callbackFlow }

        trySend(GattConnectionEvent.Connecting)
        _events.value = GattConnectionEvent.Connecting

        val cb = object : BluetoothGattCallback() {

            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
                val evt = when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> GattConnectionEvent.Connected
                    BluetoothProfile.STATE_DISCONNECTED -> GattConnectionEvent.Disconnected
                    else -> return
                }
                trySend(evt)
                _events.value = evt

                if (evt == GattConnectionEvent.Connected) {
                    val desired = 247                       // 247 byte limit on communication
                    g.requestMtu(desired)
                }

            }

            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i(TAG, "Negotiated MTU = $mtu")
                }
                // now it is safe to discover services
                g.discoverServices()
            }

            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    trySend(GattConnectionEvent.Error("Service discovery failed ($status)")); return
                }

                rxChar = g.getService(MESH_SERVICE_UUID)?.getCharacteristic(MESH_RX_CHAR_UUID)
                if (rxChar == null) {
                    trySend(GattConnectionEvent.Error("RX characteristic not found")); return
                }

                g.setCharacteristicNotification(rxChar, true)

                cccd = rxChar!!.getDescriptor(CCCD_UUID)
                if (cccd == null) {
                    trySend(GattConnectionEvent.Error("CCCD missing on peripheral")); return
                }

                val enable =
                    if (rxChar!!.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0)
                        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    else BluetoothGattDescriptor.ENABLE_INDICATION_VALUE

                val ok = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    g.writeDescriptor(cccd!!, enable) == SUCCESS
                else {
                    // Likely failed as API < 33, try old API
                    cccd!!.value = enable
                    g.writeDescriptor(cccd!!)
                }

                if (!ok) trySend(GattConnectionEvent.Error("CCCD write request failed"))
            }

            override fun onDescriptorWrite(
                g: BluetoothGatt,
                d: BluetoothGattDescriptor,
                status: Int
            ) {
                if (d != cccd) return

                val evt = if (status == BluetoothGatt.GATT_SUCCESS)
                    GattConnectionEvent.ServicesDiscovered
                else
                    GattConnectionEvent.Error("CCCD write failed ($status)")

                trySend(evt)
                _events.value = evt
            }


            override fun onCharacteristicChanged(
                g: BluetoothGatt,
                ch: BluetoothGattCharacteristic,
                value: ByteArray
            ) {
                if (ch.uuid == MESH_RX_CHAR_UUID) {
                    val snap = value.clone()
                    Log.d(
                        TAG,
                        "← RX ${snap.size} B  ${snap.joinToString(" ") { "%02X".format(it) }}"
                    )
                    incoming.tryEmit(snap)
                }
            }

            override fun onCharacteristicWrite(
                g: BluetoothGatt,
                ch: BluetoothGattCharacteristic,
                status: Int
            ) {
//                val evt = if (status == BluetoothGatt.GATT_SUCCESS)
//                    GattConnectionEvent.WriteCompleted(ch.uuid)
//                else
//                    GattConnectionEvent.Error("Write failed: $status")
//
//                trySend(evt) // should not send
//                _events.value = evt // should not send
                currentAck?.run {
                    if (status == BluetoothGatt.GATT_SUCCESS) complete(Unit)
                    else completeExceptionally(IOException("Write failed ($status)"))
                }
                currentAck = null
            }
        }

        gatt = device.connectGatt(ctx, false, cb)

        /* -------- START / STOP the writer coroutine --------------- */
        writerJob = CoroutineScope(Dispatchers.IO).launch {
            for (p in outQ) {
                try {
                    writeBlocking(p.bytes)          // ② wraps platform call + waits
                    currentAck = p.ack              // wait for callback to resolve it
                    p.ack.await()                   // suspend until onCharacteristicWrite()
                } catch (t: Throwable) {
                    p.ack.completeExceptionally(t)
                }
            }
        }

//        awaitClose {
//            writerJob?.cancel()
//            outQ.close()
//            gatt?.disconnect(); gatt?.close()
//            gatt = null; rxChar = null; cccd = null
//        }
        awaitClose { cleanup() }
    }

    /* --------------------------------------------------------------------- */
    /*  disconnect / send / flows                          */
    /* --------------------------------------------------------------------- */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun disconnect() = cleanup()

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private suspend fun writeBlocking(bytes: ByteArray) {
        val g = gatt ?: throw IllegalStateException("Not connected")
        val tx = g.getService(MESH_SERVICE_UUID)
            ?.getCharacteristic(MESH_TX_CHAR_UUID)
            ?: throw IllegalStateException("TX char not found")

        val ok = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            g.writeCharacteristic(
                tx,
                bytes,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            ) == SUCCESS
        else {
            tx.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            tx.value = bytes
            g.writeCharacteristic(tx)
        }
        if (!ok) throw IOException("writeCharacteristic busy / failed")
    }

    @Synchronized                         // avoid races between connect / disconnect
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun cleanup() {
        writerJob?.cancel(); writerJob = null
        outQ.close()                      // drop pending writes
        outQ = Channel(Channel.UNLIMITED)
        currentAck?.completeExceptionally(IOException("Disconnected")); currentAck = null

        gatt?.apply {                     // close politely
            try {
                disconnect()
            } catch (_: Exception) {
            }
            try {
                close()
            } catch (_: Exception) {
            }
        }
        gatt = null
        rxChar = null
        cccd = null

        // Emit a final Disconnected event so UI state resets immediately
        _events.value = GattConnectionEvent.Disconnected
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override suspend fun sendMessage(payload: ByteArray) {
//        val g = gatt ?: throw IllegalStateException("Not connected")
//
//        val tx = g.getService(MESH_SERVICE_UUID)
//            ?.getCharacteristic(MESH_TX_CHAR_UUID)
//            ?: throw IllegalStateException("TX characteristic not found")
//
//        val ok = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
//            g.writeCharacteristic(tx, payload, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) == SUCCESS
//        else {
//            // Likely failed as API < 33, try old API
//            tx.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
//            tx.value     = payload
//            g.writeCharacteristic(tx)
//        }
//
//        if (!ok) throw IOException("writeCharacteristic request failed")
        val ack = CompletableDeferred<Unit>()
        outQ.send(Pending(payload, ack))            // back-pressures if queue is full
        ack.await()                                 // returns when write done or fails
    }

    override fun incomingMessages(): Flow<ByteArray> = incoming.asSharedFlow()
    override fun connectionEvents(): StateFlow<GattConnectionEvent?> = _events

    companion object {
        val MESH_SERVICE_UUID: UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
        val MESH_TX_CHAR_UUID: UUID = UUID.fromString("0000feed-0001-1000-8000-00805f9b34fb")
        val MESH_RX_CHAR_UUID: UUID = UUID.fromString("0000feed-0002-1000-8000-00805f9b34fb")
    }
}
