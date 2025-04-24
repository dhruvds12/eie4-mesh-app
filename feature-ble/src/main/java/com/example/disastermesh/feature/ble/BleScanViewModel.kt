package com.example.disastermesh.feature.ble

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.disastermesh.core.ble.BleDevice
import com.example.disastermesh.core.ble.BleScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class BleScanViewModel @Inject constructor(
    bleScanner: BleScanner
) : ViewModel() {

    /** Exposed as a cold Flow so UI can collectLatest. */
    val devices: StateFlow<List<BleDevice>> =
        bleScanner.startScan()
            .scan(emptyList<BleDevice>()) { list, device ->
                // keep unique by address:
                (list + device).distinctBy { it.address }
            }
            .stateIn(
                scope     = viewModelScope,
                started   = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
}