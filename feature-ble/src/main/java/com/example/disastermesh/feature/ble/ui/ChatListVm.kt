package com.example.disastermesh.feature.ble.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.disastermesh.core.data.u32
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatListVm @Inject constructor(
    private val repo: com.example.disastermesh.core.ble.repository.MeshRepository,
    private val dao : com.example.disastermesh.core.database.dao.ChatDao
) : ViewModel() {

    fun chats(type: String) = when (type) {
        "NODE" -> repo.chats(com.example.disastermesh.core.database.MessageType.NODE)
        "USER" -> repo.chats(com.example.disastermesh.core.database.MessageType.USER)
        else   -> flowOf(emptyList())
    }

    /** upsert a USER chat row from a phone number */
    fun createUserChat(e164: String) = viewModelScope.launch {
        val uid   = com.example.disastermesh.core.ble.phoneToUserId(e164)
        val cid   = com.example.disastermesh.core.ble.makeChatId(
            com.example.disastermesh.core.database.MessageType.USER, uid
        )
        dao.upsertChat(
            com.example.disastermesh.core.database.entities.Chat(
                id = cid,
                type = com.example.disastermesh.core.database.MessageType.USER,
                title = "User ${uid.u32}"
            )
        )
        _newChat.emit(cid)                      // notify UI
    }

    /* ---- one‑shot flow so screen can navigate ----------------------- */
    private val _newChat = kotlinx.coroutines.flow.MutableSharedFlow<Long>()
    val newChat = _newChat
}
