package com.example.disastermesh.core.ble

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import androidx.annotation.RequiresPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject


class AndroidGattManager @Inject constructor(
    @ApplicationContext private val ctx: Context
) : GattManager {

    private var gatt: BluetoothGatt? = null

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun connect(address: String) = callbackFlow {
        val device = (ctx.getSystemService(BluetoothManager::class.java))
            ?.adapter?.getRemoteDevice(address)
            ?: run { trySend(GattConnectionEvent.Error("Device not found")); close(); return@callbackFlow }

        trySend(GattConnectionEvent.Connecting)
        gatt = device.connectGatt(ctx, false, object : BluetoothGattCallback() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    trySend(GattConnectionEvent.Connected)
                    g.discoverServices()
                } else {
                    trySend(GattConnectionEvent.Error("Disconnected or error $newState"))
                    close()
                }
            }

            override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    trySend(GattConnectionEvent.ServicesDiscovered)
                } else {
                    trySend(GattConnectionEvent.Error("Service discovery failed"))
                }
            }

            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onCharacteristicRead(
                g: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
                status: Int
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    trySend(
                        GattConnectionEvent.CharacteristicRead(
                            characteristic.uuid,
                            value
                        )
                    )
                } else {
                    trySend(GattConnectionEvent.Error("Read failed: $status"))
                }
            }


        })

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
}
