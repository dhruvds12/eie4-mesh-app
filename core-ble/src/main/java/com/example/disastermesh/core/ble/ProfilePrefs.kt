package com.example.disastermesh.core.ble

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** lightweight DataStore wrapper for the user profile */
object ProfilePrefs {

    /* ---------- DataStore instance bound to Context ------------------ */
    private val Context.dataStore by preferencesDataStore(name = "profile")

    /* ---------- preference keys -------------------------------------- */
    private val KEY_NAME  = stringPreferencesKey("name")
    private val KEY_PHONE = stringPreferencesKey("phone")

    /* ---------- public API ------------------------------------------- */
    suspend fun set(ctx: Context, name: String, phone: String) {
        ctx.dataStore.edit { pref ->
            pref[KEY_NAME]  = name
            pref[KEY_PHONE] = phone
        }
    }

    /** Emits `null` until both fields are present, then Pair(name, phone). */
    fun flow(ctx: Context): Flow<Pair<String,String>?> =
        ctx.dataStore.data.map { p ->
            val n  = p[KEY_NAME]
            val ph = p[KEY_PHONE]
            if (n == null || ph == null) null else n to ph
        }
}
