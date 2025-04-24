package com.example.disastermesh.core.ble

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import androidx.annotation.RequiresPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

// Use Inject to provide application context to constructor.
class AndroidBleScanner @Inject constructor(
    @ApplicationContext private val context: Context
) : BleScanner {

    /**
     * Caller must hold BLUETOOTH_SCAN + BLUETOOTH_CONNECT.
     * Lint will now be happy that we’ve declared them here.
     */
    @RequiresPermission(allOf = [
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    ])
    override fun startScan(): Flow<BleDevice> = callbackFlow {
        // Get the bluetooth manager and adapter
        val mgr     = context.getSystemService(BluetoothManager::class.java)
        val adapter = mgr?.adapter ?: run { close(); return@callbackFlow }
        val scanner = adapter.bluetoothLeScanner ?: run { close(); return@callbackFlow }

        // Override the callback methods for custom scanning logic
        // Callback that sends data to the flow
        val cb = object : ScanCallback() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                // emits a new bleDevice down the flow without blocking
                trySend(
                    BleDevice(
                        name    = result.device.name,
                        address = result.device.address,
                        rssi    = result.rssi
                    )
                )
            }

            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onBatchScanResults(results: List<ScanResult>) {
                results.forEach { onScanResult(0, it) }
            }

            override fun onScanFailed(errorCode: Int) {
                close(Exception("BLE scan failed: $errorCode"))
            }
        }

        // Starts the scanner with the callback
        scanner.startScan(cb)
        // waits for downstream collector to cancel. Then runs cleanup.
        awaitClose { scanner.stopScan(cb) }
    }
}
