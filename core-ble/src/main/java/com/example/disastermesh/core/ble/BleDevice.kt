package com.example.disastermesh.core.ble

/** Simple immutable holder of a BLE scan result. */
data class BleDevice(
    val name:    String?,
    val address: String,
    val rssi:    Int
)