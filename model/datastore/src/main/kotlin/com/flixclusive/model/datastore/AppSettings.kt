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

@Serializable
data class AppSettings(
    // General
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
    val providers: List<ProviderPreference> = emptyList(),
    // ==

    // == player
    val shouldReleasePlayer: Boolean = true,
    val isPlayerTimeReversed: Boolean = true,
    val preferredQuality: PlayerQuality = PlayerQuality._1080p,
    val preferredResizeMode: Int = 0, // Fit
    val preferredDiskCacheSize: Long = 0L,
    val preferredBufferCacheSize: Long = -1, // Unset
    val preferredVideoBufferMs: Long = 50_000, // 50s (Default)
    val preferredSeekAmount: Long = DEFAULT_PLAYER_SEEK_AMOUNT,
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