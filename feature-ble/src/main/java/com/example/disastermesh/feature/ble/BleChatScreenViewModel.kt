package com.example.disastermesh.feature.ble

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.disastermesh.core.ble.MeshRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject


@HiltViewModel
class BleChatViewModel @Inject constructor(
    private val meshRepo: MeshRepository
) : ViewModel() {

    val incomingMessages: StateFlow<List<String>> =
        meshRepo.incomingText()
            .runningFold(emptyList<String>()) { acc, msg -> acc + msg }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun sendMessage(text: String) {
        viewModelScope.launch {
            meshRepo.sendText(text)
        }
    }
}
