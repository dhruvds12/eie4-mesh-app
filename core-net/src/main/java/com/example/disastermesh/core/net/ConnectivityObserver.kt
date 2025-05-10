package com.example.disastermesh.core.net

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.annotation.RequiresPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ConnectivityObserver @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    private val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @get:RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    val isOnline: Flow<Boolean>
        get() = observeNetwork()

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private fun observeNetwork(): Flow<Boolean> = callbackFlow {
        fun current() = cm.activeNetwork != null

        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network)  { trySend(true) }
            override fun onLost(network: Network)       { trySend(current()) }
            override fun onUnavailable()                { trySend(false) }
        }

        trySend(current()).isSuccess
        cm.registerDefaultNetworkCallback(cb)
        awaitClose { cm.unregisterNetworkCallback(cb) }
    }
        .distinctUntilChanged()
        .conflate()
}

