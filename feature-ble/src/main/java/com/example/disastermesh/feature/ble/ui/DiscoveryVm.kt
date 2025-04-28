package com.example.disastermesh.feature.ble.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.disastermesh.core.ble.repository.BleMeshRepository
import com.example.disastermesh.core.database.MessageType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiscoveryVm @Inject constructor(
    private val repo: BleMeshRepository
) : ViewModel() {

    private val _ids   = MutableStateFlow<List<Int>?>(null)
    private val _empty = MutableStateFlow(false)

    val ids   : StateFlow<List<Int>?> = _ids
    val empty : StateFlow<Boolean>    = _empty

    fun query(t: DiscoveryType) {
        _ids.value   = null
        _empty.value = false

        viewModelScope.launch {
            when (t) {
                DiscoveryType.NODE -> {
                    repo.requestNodes()
                    repo.nodeList.firstOrNull()?.let {
                        if (it.isEmpty()) _empty.value = true else _ids.value = it
                    }
                }
                DiscoveryType.USER -> {
                    repo.requestUsers()
                    repo.userList.firstOrNull()?.let {
                        if (it.isEmpty()) _empty.value = true else _ids.value = it
                    }
                }
            }
        }
    }

    suspend fun ensureChat(id: Long, title: String, type: MessageType) =
        repo.ensureChatExists(id, title, type)

}
