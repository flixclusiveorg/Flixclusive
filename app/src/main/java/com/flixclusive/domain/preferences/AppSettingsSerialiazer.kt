package com.flixclusive.domain.preferences

import android.graphics.Color
import android.graphics.Typeface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.datastore.core.Serializer
import androidx.media3.ui.CaptionStyleCompat
import com.flixclusive_provider.models.common.MediaServer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

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

enum class CaptionEdgeTypePreference(val type: Int, val color: Int = Color.BLACK) {
    Drop_Shadow(CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW),
    Outline(CaptionStyleCompat.EDGE_TYPE_OUTLINE);
}

@Serializable
data class AppSettings(
    val preferredServer: String = MediaServer.UpCloud.serverName,
    val isSubtitleEnabled: Boolean = true,
    val subtitleColor: Int = Color.WHITE,
    val subtitleSize: CaptionSizePreference = CaptionSizePreference.Medium,
    val subtitleFontStyle: CaptionStylePreference = CaptionStylePreference.Bold,
    val subtitleBackgroundColor: Int = Color.TRANSPARENT,
    val subtitleEdgeType: CaptionEdgeTypePreference = CaptionEdgeTypePreference.Drop_Shadow,
) {
    companion object {
        fun parseFrom(input: InputStream): AppSettings {
            return Json.decodeFromString(
                deserializer = serializer(),
                string = input.readBytes().decodeToString()
            )
        }
    }
}
object AppSettingsSerializer : Serializer<AppSettings> {
    override val defaultValue: AppSettings
        get() = AppSettings()

    override suspend fun readFrom(input: InputStream): AppSettings {
        return try {
            AppSettings.parseFrom(input)
        } catch (e: SerializationException) {
            e.printStackTrace()
            defaultValue
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun writeTo(t: AppSettings, output: OutputStream) {
        output.write(
            Json.encodeToString(
                serializer = AppSettings.serializer(),
                value = t
            ).encodeToByteArray()
        )
    }
}