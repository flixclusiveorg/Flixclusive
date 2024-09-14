package com.flixclusive.model.datastore

import android.graphics.Color
import com.flixclusive.core.util.network.okhttp.USER_AGENT
import com.flixclusive.model.datastore.network.DoHPreference
import com.flixclusive.model.datastore.player.CaptionEdgeTypePreference
import com.flixclusive.model.datastore.player.CaptionSizePreference
import com.flixclusive.model.datastore.player.CaptionStylePreference
import com.flixclusive.model.datastore.player.DecoderPriority
import com.flixclusive.model.datastore.player.PlayerQuality
import kotlinx.serialization.Serializable

const val DEFAULT_PLAYER_SEEK_AMOUNT = 10000L
const val DEFAULT_PLAYER_CACHE_SIZE_AMOUNT = 200L
const val NO_LIMIT_PLAYER_CACHE_SIZE = -1L
const val DEFAULT_PLAYER_BUFFER_AMOUNT = 50L

@Serializable
data class AppSettings(
    // General
    val isUsingAutoUpdateAppFeature: Boolean = true,
    val isUsingPrereleaseUpdates: Boolean = false,
    val isSendingCrashLogsAutomatically: Boolean = true,
    // ===

    // UI
    val isShowingFilmCardTitle: Boolean = false,
    // ===

    // == Subs
    val isSubtitleEnabled: Boolean = true,
    val subtitleLanguage: String = "en",
    val subtitleColor: Int = Color.WHITE,
    val subtitleSize: CaptionSizePreference = CaptionSizePreference.Medium,
    val subtitleFontStyle: CaptionStylePreference = CaptionStylePreference.Bold,
    val subtitleBackgroundColor: Int = Color.TRANSPARENT,
    val subtitleEdgeType: CaptionEdgeTypePreference = CaptionEdgeTypePreference.Drop_Shadow,
    // ==

    // == internet/others
    val dns: DoHPreference = DoHPreference.None,
    val userAgent: String = USER_AGENT,
    // ==

    // == player
    val shouldReleasePlayer: Boolean = true,
    val isPlayerTimeReversed: Boolean = true,
    val isPiPModeEnabled: Boolean = true,
    val isUsingVolumeBoost: Boolean = false,
    val preferredAudioLanguage: String = "en",
    /** Unset = -1 = Default */
    val preferredBufferCacheSize: Long = -1,
    /** Fit = Default */
    val preferredResizeMode: Int = 0,
    /** 50s = Default */
    val preferredDiskCacheSize: Long = DEFAULT_PLAYER_CACHE_SIZE_AMOUNT,
    /** 10s = Default */
    val preferredVideoBufferMs: Long = DEFAULT_PLAYER_BUFFER_AMOUNT,
    val preferredSeekAmount: Long = DEFAULT_PLAYER_SEEK_AMOUNT,
    val preferredQuality: PlayerQuality = PlayerQuality.Quality1080p,
    val decoderPriority: DecoderPriority = DecoderPriority.PREFER_DEVICE,
    // ==
)