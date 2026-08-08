package com.flixclusive.core.network.monitor

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.NetworkRequest.Builder
import androidx.core.content.getSystemService
import com.flixclusive.core.common.dispatchers.AppDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.shareIn
import javax.inject.Inject

internal class NetworkMonitorImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    appDispatchers: AppDispatchers,
) : NetworkMonitor {
    override val isOnline: Flow<Boolean> = callbackFlow {
        val connectivityManager = context.getSystemService<ConnectivityManager>()
        if (connectivityManager == null) {
            channel.trySend(false)
            channel.close()
            return@callbackFlow
        }

        /**
         * The callback's methods are invoked on changes to *any* network matching the [NetworkRequest],
         * not just the active network. So we can simply track the presence (or absence) of such [Network].
         */
        val callback = object : ConnectivityManager.NetworkCallback() {
            private val networks = mutableSetOf<Network>()

            override fun onAvailable(network: Network) {
                networks += network
                channel.trySend(true)
            }

            override fun onLost(network: Network) {
                networks -= network
                channel.trySend(networks.isNotEmpty())
            }
        }

        val request = Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)

        /**
         * Sends the latest connectivity status to the underlying channel.
         */
        channel.trySend(connectivityManager.isCurrentlyConnected())

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.conflate()
        .shareIn(
            scope = appDispatchers.defaultScope,
            started = SharingStarted.WhileSubscribed(),
            replay = 1,
        )

    /**
     * Unlike [isOnline], which only cares whether *some* internet-capable network exists, this has
     * to track the network actually carrying traffic — a device on both Wi-Fi and mobile data is
     * unmetered, and the same Wi-Fi can flip to metered when its owner marks it so. That makes
     * `onCapabilitiesChanged` the signal rather than availability, and it means re-reading the
     * active network each time rather than remembering a set.
     */
    override val isMetered: Flow<Boolean> = callbackFlow {
        val connectivityManager = context.getSystemService<ConnectivityManager>()
        if (connectivityManager == null) {
            channel.trySend(true)
            channel.close()
            return@callbackFlow
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                channel.trySend(connectivityManager.isCurrentlyMetered())
            }

            override fun onLost(network: Network) {
                channel.trySend(connectivityManager.isCurrentlyMetered())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                channel.trySend(connectivityManager.isCurrentlyMetered())
            }
        }

        val request = Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)

        channel.trySend(connectivityManager.isCurrentlyMetered())

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.conflate()
        .shareIn(
            scope = appDispatchers.defaultScope,
            started = SharingStarted.WhileSubscribed(),
            replay = 1,
        )

    override fun isMeteredNow(): Boolean =
        context.getSystemService<ConnectivityManager>()?.isCurrentlyMetered() ?: true

    override fun isOnlineNow(): Boolean =
        context.getSystemService<ConnectivityManager>()?.isCurrentlyConnected() ?: false

    /** No active network, or no capabilities to read, counts as metered — see [NetworkMonitor.isMetered]. */
    @Suppress("DEPRECATION")
    private fun ConnectivityManager.isCurrentlyMetered(): Boolean {
        val capabilities = activeNetwork?.let(::getNetworkCapabilities) ?: return true
        return !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    @Suppress("DEPRECATION")
    private fun ConnectivityManager.isCurrentlyConnected() =
        activeNetwork
            ?.let(::getNetworkCapabilities)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
}
