package com.example.disastermesh.core.ble

import kotlinx.coroutines.flow.Flow

interface BleScanner {
    fun startScan(): Flow<BleDevice>
}