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
import androidx.media3.ui.CaptionStyleCompat
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream

@Serializable
data class AppSettings(
    val preferredQuality: String = DEFAULT_QUALITY,
    val isSubtitleEnabled: Boolean = true,
    val isShowingFilmCardTitle: Boolean = false,
    val subtitleLanguage: String = "en",
    val subtitleColor: Int = Color.WHITE,
    val subtitleSize: CaptionSizePreference = CaptionSizePreference.Medium,
    val subtitleFontStyle: CaptionStylePreference = CaptionStylePreference.Bold,
    val subtitleBackgroundColor: Int = Color.TRANSPARENT,
    val subtitleEdgeType: CaptionEdgeTypePreference = CaptionEdgeTypePreference.Drop_Shadow,
    val dns: DoHPreference = DoHPreference.None,
    val providers: List<ProviderConfiguration> = emptyList(),
) {
    companion object {
        const val DEFAULT_QUALITY = "Auto"
        val possibleAvailableQualities = listOf(
            "Auto", "1080p", "720p", "480p", "360p"
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