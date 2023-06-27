package com.flixclusive.domain.firebase

import kotlinx.coroutines.flow.StateFlow

enum class RemoteConfigStatus {
    LOADING,
    SUCCESS,
    ERROR
}

const val TMDB_API_KEY = "tmdb_api_key"
const val CONSUMET_API_HOST = "consumet_api_host"
const val CONSUMET_DEFAULT_WATCH_PROVIDER = "consumet_default_watch_provider"
const val CONSUMET_DEFAULT_VIDEO_SERVER = "consumet_default_video_server"
const val FLIXCLUSIVE_LATEST_VERSION = "flixclusive_latest_version"
const val FLIXCLUSIVE_UPDATE_URL = "flixclusive_update_url"

interface ConfigurationProvider {
    val remoteStatus: StateFlow<RemoteConfigStatus>

    val tmdbApiKey: String
    val consumetApiHost: String
    val consumetDefaultWatchProvider: String
    val consumetDefaultVideoServer: String
    val flixclusiveUpdateUrl: String
    val flixclusiveLatestVersion: Long

    fun initialize()
}