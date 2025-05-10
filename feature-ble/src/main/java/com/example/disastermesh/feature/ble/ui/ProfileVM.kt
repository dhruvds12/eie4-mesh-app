package com.example.disastermesh.feature.ble.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.disastermesh.core.ble.ProfilePrefs
import com.example.disastermesh.core.ble.phoneToUserId
import com.example.disastermesh.core.net.RegisterApi
import com.example.disastermesh.core.net.RegisterReq
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileVm @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val api: RegisterApi
) : ViewModel() {

    val profile = ProfilePrefs.flow(ctx)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun save(name: String, phone: String) = viewModelScope.launch {
        val trimmedName  = name.trim()
        val trimmedPhone = phone.trim()
        val uid          = phoneToUserId(trimmedPhone).toString()
        val body = RegisterReq(uid, trimmedPhone.filter { it.isDigit() }, trimmedName)

        runCatching { api.register(body) }
            .onSuccess { rsp ->
                Log.d("ProfileVm", "register() http ${rsp.code()}")
                if (rsp.isSuccessful) {
                    ProfilePrefs.set(ctx, trimmedName, trimmedPhone)
                } else {
                    Log.w("ProfileVm", "backend rejected: ${rsp.code()} – ${rsp.errorBody()?.string()}")
                }
            }
            .onFailure { e ->
                Log.e("ProfileVm", "register() failed", e)
            }

    }
}