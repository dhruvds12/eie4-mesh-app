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
import com.example.disastermesh.feature.ble.ui.model.ChatItem
import com.example.disastermesh.feature.ble.ui.model.withDateHeaders
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/* feature-ble/BleChatViewModel.kt */
@HiltViewModel
class BleChatViewModel @Inject constructor(
    private val repo: MeshRepository,
    @ApplicationContext ctx: Context
) : ViewModel() {

    /** profile → flow of (possibly-null) uid */
    private val uidFlow = ProfilePrefs.flow(ctx)
        .map { it?.uid }                          // uid: Int?   (null if not set yet)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /* ------------------------------------------------------------------ */
    private val chatId = MutableStateFlow<Long?>(null)
    fun setChat(id: Long) { chatId.value = id }

    /* -------- messages, with date headers ----------------------------- */
    @OptIn(ExperimentalCoroutinesApi::class)
    @RequiresApi(Build.VERSION_CODES.O)
    val items: StateFlow<List<ChatItem>> =
        chatId.filterNotNull()
            .flatMapLatest { repo.stream(it) }
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

            repo.send(cid, text, myUid)
        }
    }
}

