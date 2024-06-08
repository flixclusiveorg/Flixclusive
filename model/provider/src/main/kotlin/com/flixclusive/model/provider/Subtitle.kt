package com.flixclusive.model.provider

import com.flixclusive.model.provider.SubtitleSource.EMBEDDED
import com.flixclusive.model.provider.SubtitleSource.LOCAL
import com.flixclusive.model.provider.SubtitleSource.ONLINE
import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Enum class representing different types or sources of subtitle content.
 *
 * This enum class defines the possible sources for subtitle file, such as online subtitles,
 * locally stored files, or embedded content.
 *
 * @property ONLINE Represents subtitle file sourced from an online url.
 * @property LOCAL Represents subtitle file stored locally.
 * @property EMBEDDED Represents an embedded subtitle file, usually found in `mkv` formats.
 */
enum class SubtitleSource {
    ONLINE,
    LOCAL,
    EMBEDDED
}

/**
 * Data class representing a subtitle entity.
 *
 * This data class encapsulates information about a subtitle, including its URL, language, and source type.
 *
 * @property url The URL of the subtitle.
 * @property language The language of the subtitle.
 * @property type The source type of the subtitle, indicating whether it's online, local, or embedded. Default is [SubtitleSource.ONLINE]
 * @property customHeaders The custom headers of this link. some requests require custom authorized headers. Default is null
 */
data class Subtitle(
    val url: String,
    @SerializedName("language", alternate = ["lang"]) val language: String,
    val type: SubtitleSource = ONLINE,
    val customHeaders: Map<String, String>? = null
) : Serializable {
    override fun equals(other: Any?): Boolean {
        return try {
            other is String &&
            (url.equals(other, true) ||
            url.contains(other, true))
        } catch (_: Exception) {
            super.equals(other)
        }
    }

    override fun hashCode(): Int {
        var result = url.hashCode()
        result = 31 * result + language.hashCode()
        return result
    }
}
