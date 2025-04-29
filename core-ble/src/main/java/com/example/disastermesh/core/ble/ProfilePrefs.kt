package com.example.disastermesh.core.ble

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object ProfilePrefs {

    private val Context.dataStore by preferencesDataStore(name = "profile")

    private val KEY_NAME   = stringPreferencesKey("name")
    private val KEY_PHONE  = stringPreferencesKey("phone")
    private val KEY_USERID = intPreferencesKey   ("uid")

    suspend fun set(ctx: Context, name: String, phone: String) {
        val uid = phoneToUserId(phone)
        ctx.dataStore.edit {
            it[KEY_NAME]   = name
            it[KEY_PHONE]  = phone
            it[KEY_USERID] = uid
        }
    }

    /** emits **null** until all three fields are present */
    fun flow(ctx: Context): Flow<Profile?> =
        ctx.dataStore.data.map { p ->
            val n  = p[KEY_NAME]   ?: return@map null
            val ph = p[KEY_PHONE]  ?: return@map null
            val id = p[KEY_USERID] ?: return@map null
            Profile(n, ph, id)
        }

    data class Profile(val name: String, val phone: String, val uid: Int)
}
