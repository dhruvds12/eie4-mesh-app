package com.example.disastermesh.core.ble

import kotlinx.coroutines.flow.Flow

interface BleScanner {
    /**
   * Returns a cold Flow that, once collected, starts a BLE scan.
   * Emits a stream of BleDevice as results arrive.
   * Completes only when the collector cancels the Flow or scanner errors out.
     */
    fun startScan(): Flow<BleDevice>
}