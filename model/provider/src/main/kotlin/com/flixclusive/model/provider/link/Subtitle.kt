package com.flixclusive.model.provider.link

import com.flixclusive.model.provider.link.SubtitleSource.ONLINE
import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Data class representing a subtitle entity.
 *
 * This data class encapsulates information about a subtitle, including its URL, language, and source type.
 *
 * @property url The URL of the subtitle.
 * @property language The language of the subtitle.
 * @property type The source type of the subtitle, indicating whether it's online, local, or embedded. Default is [SubtitleSource.ONLINE]
 * @property flags A set of resource [Flag]s associated with the subtitle. Default is null
 *
 * @property name The name of the subtitle, derived from the [language].
 */
data class Subtitle(
    @SerializedName("language", alternate = ["lang"]) val language: String,
    val type: SubtitleSource = ONLINE,
    override val url: String,
    override val flags: Set<Flag>? = null,
) : Serializable, MediaLink() {
    override val name: String
        get() = language

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
