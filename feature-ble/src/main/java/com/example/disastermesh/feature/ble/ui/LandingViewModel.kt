package com.example.disastermesh.feature.ble.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.disastermesh.core.ble.GattConnectionEvent
import com.example.disastermesh.core.ble.GattManager
import com.example.disastermesh.core.ble.ProfilePrefs
import com.example.disastermesh.core.ble.repository.BleMeshRepository
import com.example.disastermesh.core.crypto.CryptoBox
import com.example.disastermesh.core.data.MessageCodec
import com.example.disastermesh.core.net.ConnectivityObserver
import com.example.disastermesh.core.net.UserNetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LandingViewModel @Inject constructor(
    private val gatt: GattManager,
    net: ConnectivityObserver,
    private val netRepo: UserNetRepository,
    meshRepo: BleMeshRepository,
    @ApplicationContext val ctx: Context
) : ViewModel() {

    private val _connection = MutableStateFlow<GattConnectionEvent?>(null)
    val      connection : StateFlow<GattConnectionEvent?> = _connection.asStateFlow()

    private val profileFlow = ProfilePrefs.flow(ctx)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val profile = profileFlow

    val nodeId: StateFlow<Int?> = meshRepo.nodeIdFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var connectJob: Job? = null

    init {
        gatt.connectionEvents()
            .onEach { _connection.value = it }
            .launchIn(viewModelScope)
    }

    init {
        profileFlow.filterNotNull()
            .onEach { p -> netRepo.start(p.uid, viewModelScope) }
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
//                        gatt.sendMessage(encodeUserIdUpdate(p.uid))

                        val pk32 = CryptoBox.ensureKeyPair(p.uid, ctx)
                        gatt.sendMessage(
                            MessageCodec.encodeAnnounceKey(p.uid, pk32)
                        )
                    }

                    profileFlow.value?.let { p ->
                        runCatching {
                            gatt.sendMessage(MessageCodec.encodeRequestNodeId(p.uid))
                        }
                    }
                }
            }
        }
    }


    fun disconnect() {
        gatt.disconnect()
        connectJob?.cancel()     // stop collecting the *old* flow
        connectJob = null
    }

    // --------------- NET -------------------------

    private val modeFlow = combine(
        connection.map { evt ->                     // BLE connected?
            evt == GattConnectionEvent.ServicesDiscovered ||
                    evt is GattConnectionEvent.WriteCompleted
        },
        net.isOnline,                               // Internet reachable?
        profileFlow.map { it != null }              // Profile present?
    ) { ble, online, hasProfile ->
        UiState(ble, online, hasProfile)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState())

    /** public read‑only */
    val ui: StateFlow<UiState> = modeFlow
}

data class UiState(
    val bleConnected: Boolean = false,
    val internet: Boolean     = false,
    val hasProfile: Boolean   = false
) {
    enum class Mode { BLE, NET, BOTH, OFF }
    val mode: Mode get() = when {
        bleConnected && internet -> Mode.BOTH
        bleConnected            -> Mode.BLE
        internet                -> Mode.NET
        else                    -> Mode.OFF
    }
}
