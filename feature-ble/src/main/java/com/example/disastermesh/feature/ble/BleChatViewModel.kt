package com.example.disastermesh.feature.ble

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.disastermesh.core.ble.repository.MeshRepository
import com.example.disastermesh.core.database.entities.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BleChatViewModel @Inject constructor(
    private val repo: MeshRepository
) : ViewModel() {

    private companion object { const val MY_USER_ID = 0 }

    private val chatId = MutableStateFlow<Long?>(null)
    fun setChat(id: Long) { chatId.value = id }

    val messages: StateFlow<List<Message>> =
        chatId.filterNotNull()
            .flatMapLatest(repo::stream)
            .stateIn(viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList())


    /*  send ------------------------------------------------------------ */
    fun send(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            chatId.value?.let { id -> repo.send(id, text, MY_USER_ID) }
        }
    }
}
