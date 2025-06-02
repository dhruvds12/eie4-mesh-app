package com.example.disastermesh.core.ble

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.nodeStore by preferencesDataStore("node_prefs")
private val LAST_NODE = intPreferencesKey("lastNodeId")

@Singleton
class NodePrefs @Inject constructor(
    @ApplicationContext ctx: Context
) {
    private val ds = ctx.nodeStore

    /** `null` until we have seen the first NODE_ID frame */
    val lastNodeFlow: Flow<Int?> = ds.data.map { it[LAST_NODE] }

    suspend fun set(nodeId: Int) = ds.edit { it[LAST_NODE] = nodeId }
}
