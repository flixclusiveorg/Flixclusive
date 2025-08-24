package com.flixclusive.core.datastore.model.user

import com.flixclusive.core.datastore.model.user.player.DecoderPriority
import com.flixclusive.core.datastore.model.user.player.PlayerQuality
import com.flixclusive.core.datastore.model.user.player.ResizeMode
import kotlinx.serialization.Serializable

const val DEFAULT_PLAYER_SEEK_AMOUNT = 10000L
const val DEFAULT_PLAYER_CACHE_SIZE_AMOUNT = 200L
const val NO_LIMIT_PLAYER_CACHE_SIZE = -1L
const val DEFAULT_PLAYER_BUFFER_AMOUNT = 50L

@Serializable
data class PlayerPreferences(
    val isForcingPlayerRelease: Boolean = true,
    val isDurationReversed: Boolean = true,
    val isPiPModeEnabled: Boolean = true,
    val isUsingVolumeBoost: Boolean = false,
    val audioLanguage: String = "en",
    /** Unset = -1 = Default */
    val bufferCacheSize: Long = -1,
    /** Fit = Default */
    val resizeMode: ResizeMode = ResizeMode.Fit,
    /** 50s = Default */
    val diskCacheSize: Long = DEFAULT_PLAYER_CACHE_SIZE_AMOUNT,
    /** 10s = Default */
    val videoBufferMs: Long = DEFAULT_PLAYER_BUFFER_AMOUNT,
    val seekAmount: Long = DEFAULT_PLAYER_SEEK_AMOUNT,
    val quality: PlayerQuality = PlayerQuality.Quality1080p,
    val decoderPriority: DecoderPriority = DecoderPriority.PREFER_DEVICE,
) : UserPreferences
