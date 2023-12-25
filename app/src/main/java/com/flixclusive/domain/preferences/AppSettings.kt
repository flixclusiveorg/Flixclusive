package com.flixclusive.domain.preferences

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import com.flixclusive.presentation.mobile.screens.player.DEFAULT_PLAYER_SEEK_AMOUNT
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream

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
    val providers: List<ProviderConfiguration> = emptyList(),
    // ==

    // == player
    val shouldReleasePlayer: Boolean = true,
    val isPlayerTimeReversed: Boolean = true,
    val preferredQuality: String = DEFAULT_QUALITY,
    val preferredResizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
    val preferredDiskCacheSize: Long = 0L,
    val preferredBufferCacheSize: Long = DefaultLoadControl.DEFAULT_TARGET_BUFFER_BYTES.toLong(),
    val preferredVideoBufferMs: Long = DefaultLoadControl.DEFAULT_MAX_BUFFER_MS.toLong(),
    val preferredSeekAmount: Long = DEFAULT_PLAYER_SEEK_AMOUNT,
    // ==
) {
    companion object {
        const val DEFAULT_QUALITY = "1080p"
        val possibleAvailableQualities = listOf(
            "4k", "1080p", "720p", "480p", "360p"
        )

        val possibleAvailableSeekIncrementMs = listOf(
            5L, 10L, 30L
        )

        val resizeModes = mapOf(
            "Fit" to AspectRatioFrameLayout.RESIZE_MODE_FIT,
            "Stretch" to AspectRatioFrameLayout.RESIZE_MODE_FILL,
            "Center Crop" to AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
        )

        enum class CaptionSizePreference {
            Small,
            Medium,
            Large;

            companion object {
                private const val SMALL_FONT_SIZE = 18F // Equivalent to 20dp
                private const val MEDIUM_FONT_SIZE = 20F // Equivalent to 20dp
                private const val LARGE_FONT_SIZE = 24F

                fun CaptionSizePreference.getDp(isTv: Boolean = false): Float {
                    return when(this) {
                        Large -> LARGE_FONT_SIZE + (if(isTv) 4F else 0F)
                        Medium -> MEDIUM_FONT_SIZE + (if(isTv) 4F else 0F)
                        Small -> SMALL_FONT_SIZE + (if(isTv) 2F else 0F)
                    }
                }
            }
        }

        enum class CaptionStylePreference(val typeface: Typeface) {
            Normal(Typeface.create("sans-serif", Typeface.NORMAL)),
            Bold(Typeface.create("sans-serif-medium", Typeface.BOLD)),
            Italic(Typeface.create("sans-serif-medium", Typeface.ITALIC)),
            Monospace(Typeface.create("monospace", Typeface.NORMAL));

            companion object {
                @Composable
                fun CaptionStylePreference.getTextStyle(): TextStyle {
                    return when(this) {
                        Normal -> MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal)
                        Bold -> MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        Italic -> MaterialTheme.typography.labelLarge.copy(fontStyle = FontStyle.Italic)
                        Monospace -> MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        @Suppress("EnumEntryName")
        @SuppressLint("UnsafeOptInUsageError")
        enum class CaptionEdgeTypePreference(val type: Int, val color: Int = Color.BLACK) {
            Drop_Shadow(CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW),
            Outline(CaptionStyleCompat.EDGE_TYPE_OUTLINE);
        }

        @Suppress("SpellCheckingInspection")
        enum class DoHPreference {
            None,
            Google,
            Cloudfare,
            AdGuard,
            Quad9,
            AliDNS,
            DNSPod,
            DNS360,
            Quad101,
            Mullvad,
            ControlD,
            Najalla,
            SheCan;
        }

        fun parseFrom(input: InputStream): AppSettings {
            return Json.decodeFromString(
                deserializer = serializer(),
                string = input.readBytes().decodeToString()
            )
        }
    }
}