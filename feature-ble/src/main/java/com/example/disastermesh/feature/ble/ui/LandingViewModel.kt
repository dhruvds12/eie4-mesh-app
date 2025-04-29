package com.example.disastermesh.feature.ble.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.disastermesh.core.ble.GattConnectionEvent
import com.example.disastermesh.core.ble.GattManager
import com.example.disastermesh.core.ble.ProfilePrefs
import com.example.disastermesh.core.ble.encodeUserIdUpdate
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LandingViewModel @Inject constructor(
    private val gatt: GattManager,
    @ApplicationContext ctx: Context
) : ViewModel() {

    private val _connection = MutableStateFlow<GattConnectionEvent?>(null)
    val      connection : StateFlow<GattConnectionEvent?> = _connection.asStateFlow()

    private val profileFlow = ProfilePrefs.flow(ctx)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val profile = profileFlow

    private var connectJob: Job? = null

    init {
        gatt.connectionEvents()
            .onEach { _connection.value = it }
            .launchIn(viewModelScope)
    }

    fun connect(addr: String) {
        if (connectJob?.isActive == true) return
        connectJob = viewModelScope.launch {
            gatt.connect(addr).collect { evt ->
                _connection.value = evt

                if (evt == GattConnectionEvent.ServicesDiscovered) {
                    /* first moment the link is ready – push USER_ID_UPDATE */
                    profileFlow.value?.let { p ->
                        gatt.sendMessage(encodeUserIdUpdate(p.uid))
                    }
                }
            }
        }
    }


    fun disconnect() {
        gatt.disconnect()
    }
}
