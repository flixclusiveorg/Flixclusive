package com.flixclusive.core.datastore.migration.model

import android.graphics.Color
import com.flixclusive.core.datastore.model.user.DEFAULT_PLAYER_BUFFER_AMOUNT
import com.flixclusive.core.datastore.model.user.DEFAULT_PLAYER_CACHE_SIZE_AMOUNT
import com.flixclusive.core.datastore.model.user.DEFAULT_PLAYER_SEEK_AMOUNT
import com.flixclusive.core.datastore.model.user.network.DoHPreference
import com.flixclusive.core.datastore.model.user.player.CaptionEdgeTypePreference
import com.flixclusive.core.datastore.model.user.player.CaptionSizePreference
import com.flixclusive.core.datastore.model.user.player.CaptionStylePreference
import com.flixclusive.core.datastore.model.user.player.DecoderPriority
import com.flixclusive.core.datastore.model.user.player.PlayerQuality
import com.flixclusive.core.util.network.okhttp.USER_AGENT
import kotlinx.serialization.Serializable

@Serializable
internal data class OldAppSettings(
    val isUsingAutoUpdateAppFeature: Boolean = true,
    val isUsingPrereleaseUpdates: Boolean = false,
    val isSendingCrashLogsAutomatically: Boolean = true,
    val isShowingFilmCardTitle: Boolean = false,
    val isSubtitleEnabled: Boolean = true,
    val subtitleLanguage: String = "en",
    val subtitleColor: Int = Color.WHITE,
    val subtitleSize: CaptionSizePreference = CaptionSizePreference.Medium,
    val subtitleFontStyle: CaptionStylePreference = CaptionStylePreference.Bold,
    val subtitleBackgroundColor: Int = Color.TRANSPARENT,
    val subtitleEdgeType: CaptionEdgeTypePreference = CaptionEdgeTypePreference.Drop_Shadow,
    val dns: DoHPreference = DoHPreference.None,
    val userAgent: String = USER_AGENT,
    // ==

    val isIncognito: Boolean = false,

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
