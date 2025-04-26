package com.example.disastermesh.feature.ble.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.disastermesh.core.ble.GattConnectionEvent
import com.example.disastermesh.core.ble.GattManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LandingViewModel @Inject constructor(
    private val gatt: GattManager
) : ViewModel() {

    private val _connection = MutableStateFlow<GattConnectionEvent?>(null)
    val      connection : StateFlow<GattConnectionEvent?> = _connection.asStateFlow()

    private var connectJob: Job? = null

    init {
        gatt.connectionEvents()
            .onEach { _connection.value = it }
            .launchIn(viewModelScope)
    }

    fun connect(address: String) {
        connectJob?.cancel()

        connectJob = viewModelScope.launch {
            gatt.connect(address).collect()
        }
    }


    fun disconnect() {
        gatt.disconnect()
    }
}
