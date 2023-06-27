package com.flixclusive.data.firebase

import com.flixclusive.data.api.utils.HostSelectionInterceptor
import com.flixclusive.domain.firebase.CONSUMET_API_HOST
import com.flixclusive.domain.firebase.CONSUMET_DEFAULT_VIDEO_SERVER
import com.flixclusive.domain.firebase.CONSUMET_DEFAULT_WATCH_PROVIDER
import com.flixclusive.domain.firebase.ConfigurationProvider
import com.flixclusive.domain.firebase.FLIXCLUSIVE_LATEST_VERSION
import com.flixclusive.domain.firebase.FLIXCLUSIVE_UPDATE_URL
import com.flixclusive.domain.firebase.RemoteConfigStatus
import com.flixclusive.domain.firebase.TMDB_API_KEY
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class ConfigurationProviderImpl @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig,
    private val hostSelectionInterceptor: HostSelectionInterceptor
): ConfigurationProvider {
    private val _remoteStatus = MutableStateFlow(RemoteConfigStatus.LOADING)
    override val remoteStatus: StateFlow<RemoteConfigStatus>
        get() = _remoteStatus.asStateFlow()

    override val tmdbApiKey: String
        get() = remoteConfig.getString(TMDB_API_KEY)
    override val consumetApiHost: String
        get() = remoteConfig.getString(CONSUMET_API_HOST)
    override val consumetDefaultWatchProvider: String
        get() = remoteConfig.getString(CONSUMET_DEFAULT_WATCH_PROVIDER)
    override val consumetDefaultVideoServer: String
        get() = remoteConfig.getString(CONSUMET_DEFAULT_VIDEO_SERVER)
    override val flixclusiveUpdateUrl: String
        get() = remoteConfig.getString(FLIXCLUSIVE_UPDATE_URL)
    override val flixclusiveLatestVersion: Long
        get() = remoteConfig.getLong(FLIXCLUSIVE_LATEST_VERSION)

    override fun initialize() {
        _remoteStatus.update { RemoteConfigStatus.LOADING }
        remoteConfig.fetchAndActivate()
            .addOnSuccessListener {
                // Update the App's API consumetHost
                hostSelectionInterceptor.updateHost(consumetApiHost)

                _remoteStatus.update { RemoteConfigStatus.SUCCESS }
            }.addOnFailureListener {
                _remoteStatus.update { RemoteConfigStatus.ERROR }
            }
    }
}