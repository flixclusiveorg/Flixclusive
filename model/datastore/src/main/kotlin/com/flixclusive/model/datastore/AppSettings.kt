package com.flixclusive.model.datastore

import android.graphics.Color
import com.flixclusive.model.datastore.network.DoHPreference
import com.flixclusive.model.datastore.player.CaptionEdgeTypePreference
import com.flixclusive.model.datastore.player.CaptionSizePreference
import com.flixclusive.model.datastore.player.CaptionStylePreference
import com.flixclusive.model.datastore.player.PlayerQuality
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream

const val DEFAULT_PLAYER_SEEK_AMOUNT = 10000L
const val DEFAULT_PLAYER_CACHE_SIZE_AMOUNT = 200L
const val NO_LIMIT_PLAYER_CACHE_SIZE = -1L
const val DEFAULT_PLAYER_BUFFER_AMOUNT = 50L

@Serializable
data class AppSettings(
    // General
    val isUsingAutoUpdateAppFeature: Boolean = true,
    val isUsingAutoUpdateProviderFeature: Boolean = true,
    val isUsingPrereleaseUpdates: Boolean = false,
    val isSendingCrashLogsAutomatically: Boolean = true,
    // ===

    // UI
    val isShowingFilmCardTitle: Boolean = false,
    // ===

    // On-boarding settings
    val isFirstTimeUserLaunch_: Boolean = true, // TODO: Remove underscore
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
    // ==

    // == player
    /**
     * Default value should always be true!
     * */
    val shouldReleasePlayer: Boolean = true,
    val isPlayerTimeReversed: Boolean = true,
    val isUsingVolumeBoost: Boolean = false,
    val preferredAudioLanguage: String = "en",
    /**
     * 1080p is the default value
     * */
    val preferredQuality: PlayerQuality = PlayerQuality.Quality1080p,
    /**
     * Fit screen is the default value for resize mode
     * */
    val preferredResizeMode: Int = 0, // Fit
    /**
     * 50s = Default
     * */
    val preferredDiskCacheSize: Long = DEFAULT_PLAYER_CACHE_SIZE_AMOUNT,
    val preferredBufferCacheSize: Long = -1, // Unset
    /**
     * 10s = Default
     * */
    val preferredVideoBufferMs: Long = DEFAULT_PLAYER_BUFFER_AMOUNT,
    /**
     * Fit screen is the default value for resize mode
     * */
    val preferredSeekAmount: Long = DEFAULT_PLAYER_SEEK_AMOUNT,
    /**
     *
     * required for cache warnings, when [preferredDiskCacheSize]
     * is set to -1, which means there is no limit set for caching.
     * */
    val shouldNotifyAboutCache: Boolean = true,
    // ==
) {
    companion object {
        val possibleAvailableSeekIncrementMs = listOf(
            5L, 10L, 30L
        )

        fun parseFrom(input: InputStream): AppSettings {
            return Json.decodeFromString(
                deserializer = serializer(),
                string = input.readBytes().decodeToString()
            )
        }
    }
}