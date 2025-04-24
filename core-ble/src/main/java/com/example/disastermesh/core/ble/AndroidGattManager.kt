package com.example.disastermesh.core.ble

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import androidx.annotation.RequiresPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidGattManager @Inject constructor(
    @ApplicationContext private val ctx: Context
) : GattManager {

    private var gatt: BluetoothGatt? = null
    private val incoming = MutableSharedFlow<ByteArray>()

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun connect(address: String): Flow<GattConnectionEvent> = callbackFlow {
        val mgr = ctx.getSystemService(BluetoothManager::class.java)
        val device = mgr?.adapter?.getRemoteDevice(address)
            ?: run {
                trySend(GattConnectionEvent.Error("Device not found"))
                close()
                return@callbackFlow
            }

        trySend(GattConnectionEvent.Connecting)

        val cb = object : BluetoothGattCallback() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onConnectionStateChange(
                g: BluetoothGatt, status: Int, newState: Int
            ) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        trySend(GattConnectionEvent.Connected)
                        g.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        trySend(GattConnectionEvent.Disconnected)
                        close()
                    }
                }
            }

            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    // subscribe to notifications
                    val svc = g.getService(MESH_SERVICE_UUID) ?: return
                    val rx  = svc.getCharacteristic(MESH_RX_CHAR_UUID) ?: return
                    g.setCharacteristicNotification(rx, true)
                    val desc = rx.getDescriptor(CLIENT_CONFIG_UUID)
                    desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    g.writeDescriptor(desc)

                    trySend(GattConnectionEvent.ServicesDiscovered)
                } else {
                    trySend(GattConnectionEvent.Error("Service discovery failed"))
                }
            }

            override fun onCharacteristicChanged(
                g: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
            ) {
                if (characteristic.uuid == MESH_RX_CHAR_UUID) {
                    incoming.tryEmit(characteristic.value)
                }
            }

            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onCharacteristicWrite(
                g: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    trySend(GattConnectionEvent.WriteCompleted(characteristic.uuid))
                } else {
                    trySend(GattConnectionEvent.Error("Write failed: $status"))
                }
            }
        }

        gatt = device.connectGatt(ctx, false, cb)

        awaitClose {
            gatt?.disconnect()
            gatt?.close()
            gatt = null
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun disconnect() {
        gatt?.disconnect()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override suspend fun sendMessage(payload: ByteArray) {
        val currentGatt = gatt ?: throw IllegalStateException("Not connected")

        val svc = currentGatt.getService(MESH_SERVICE_UUID)
            ?: throw IllegalStateException("Service not found")

        val tx = svc.getCharacteristic(MESH_TX_CHAR_UUID)
            ?: throw IllegalStateException("TX characteristic not found")

        tx.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        tx.value = payload

        if (!currentGatt.writeCharacteristic(tx)) {
            throw IOException("writeCharacteristic failed")
        }
    }

    override fun incomingMessages(): Flow<ByteArray> = incoming

    companion object {
        val MESH_SERVICE_UUID  = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
        val MESH_TX_CHAR_UUID  = UUID.fromString("0000feed-0001-1000-8000-00805f9b34fb")
        val MESH_RX_CHAR_UUID  = UUID.fromString("0000feed-0002-1000-8000-00805f9b34fb")
        val CLIENT_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}

