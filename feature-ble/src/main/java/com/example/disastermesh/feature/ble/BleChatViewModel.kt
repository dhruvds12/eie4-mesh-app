package com.example.disastermesh.feature.ble

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.disastermesh.core.ble.ProfilePrefs
import com.example.disastermesh.core.ble.idType
import com.example.disastermesh.core.ble.repository.MeshRepository
import com.example.disastermesh.core.database.MessageType
import com.example.disastermesh.core.database.entities.Route
import com.example.disastermesh.core.net.ConnectivityObserver
import com.example.disastermesh.core.net.UserNetRepository
import com.example.disastermesh.feature.ble.ui.model.ChatItem
import com.example.disastermesh.feature.ble.ui.model.withDateHeaders
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BleChatViewModel @Inject constructor(
    val bleRepo: MeshRepository,
    private val netRepo: UserNetRepository,
    private val conn: ConnectivityObserver,
    @ApplicationContext ctx: Context
) : ViewModel() {
    /** profile → flow of (possibly-null) uid */
    private val uidFlow = ProfilePrefs.flow(ctx)
        .map { it?.uid }                          // uid: Int?   (null if not set yet)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /* ------------------------------------------------------------------ */
    private val _currentRoute = MutableStateFlow(Route.MESH)
    val currentRoute: StateFlow<Route> = _currentRoute

    private val chatId = MutableStateFlow<Long?>(null)

    init {
        /* when chat changes, load route once */
        chatId.filterNotNull()
            .onEach { cid ->
                _currentRoute.value = bleRepo.getRoute(cid) ?: Route.MESH
            }.launchIn(viewModelScope)

        /* live update when repo emits a change */
        bleRepo.routeUpdates.onEach { (cid, r) ->
            if (cid == chatId.value) _currentRoute.value = r
        }.launchIn(viewModelScope)
    }

    fun setChat(id: Long) { chatId.value = id }

    fun setRoute(r: Route) = viewModelScope.launch {
        chatId.value?.let { cid ->
            bleRepo.setRoute(cid, r)
            _currentRoute.value = r
        }
    }

    /* -------- messages, with date headers ----------------------------- */
    @OptIn(ExperimentalCoroutinesApi::class)
    @RequiresApi(Build.VERSION_CODES.O)
    val items: StateFlow<List<ChatItem>> =
        chatId.filterNotNull()
            .flatMapLatest { bleRepo.stream(it) }
            .map { it.withDateHeaders() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /* -------- send ----------------------------------------------------- */
    fun send(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            val cid = chatId.value ?: return@launch
            val currentType  = idType(cid)

            val myUid = when {
                /* user↔user needs a real UID – abort if none */
                currentType == MessageType.USER  && uidFlow.value == null -> return@launch
                /* else (broadcast / node) default to zero */
                else -> uidFlow.value ?: 0
            }

            if (currentType == MessageType.USER && conn.isOnline.first()) {
                // route over HTTP gateway
                val targetUid = (cid and 0xFFFF_FFFFL).toInt()
                netRepo.send(myUid, targetUid, cid, text)
            } else if (currentType == MessageType.USER && _currentRoute.value == Route.GATEWAY) {
                // Send via an internet gateway
                bleRepo.sendGateway(cid, text, myUid)
            } else {
                // broadcast / node / offline → BLE
                bleRepo.send(cid, text, myUid)
            }
        }
    }

}

