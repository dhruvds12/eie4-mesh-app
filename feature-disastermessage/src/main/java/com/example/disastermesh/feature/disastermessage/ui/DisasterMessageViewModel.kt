/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.disastermesh.feature.disastermessage.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.disastermesh.core.data.DisasterMessageRepository
import com.example.disastermesh.feature.disastermessage.ui.DisasterMessageUiState.Error
import com.example.disastermesh.feature.disastermessage.ui.DisasterMessageUiState.Loading
import com.example.disastermesh.feature.disastermessage.ui.DisasterMessageUiState.Success
import javax.inject.Inject

@HiltViewModel
class DisasterMessageViewModel @Inject constructor(
    private val disasterMessageRepository: DisasterMessageRepository
) : ViewModel() {

    val uiState: StateFlow<DisasterMessageUiState> = disasterMessageRepository
        .disasterMessages.map<List<String>, DisasterMessageUiState> { Success(data = it) }
        .catch { emit(Error(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Loading)

    fun addDisasterMessage(name: String) {
        viewModelScope.launch {
            disasterMessageRepository.add(name)
        }
    }
}

sealed interface DisasterMessageUiState {
    object Loading : DisasterMessageUiState
    data class Error(val throwable: Throwable) : DisasterMessageUiState
    data class Success(val data: List<String>) : DisasterMessageUiState
}
