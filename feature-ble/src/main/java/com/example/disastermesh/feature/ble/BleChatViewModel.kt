package com.example.disastermesh.feature.ble

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.disastermesh.core.ble.repository.MeshRepository
import com.example.disastermesh.feature.ble.ui.model.ChatItem
import com.example.disastermesh.feature.ble.ui.model.withDateHeaders
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    @OptIn(ExperimentalCoroutinesApi::class)
    @RequiresApi(Build.VERSION_CODES.O)
    val items: StateFlow<List<ChatItem>> =
        chatId.filterNotNull()
            .flatMapLatest { id -> repo.stream(id) }
            .map { list -> list.withDateHeaders() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())


    /*  send ------------------------------------------------------------ */
    fun send(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            chatId.value?.let { id -> repo.send(id, text, MY_USER_ID) }
        }
    }
}
