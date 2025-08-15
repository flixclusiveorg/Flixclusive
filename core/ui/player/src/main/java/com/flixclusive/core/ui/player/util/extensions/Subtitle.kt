package com.flixclusive.core.ui.player.util.extensions

import android.net.Uri
import android.webkit.URLUtil
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastMapNotNull
import androidx.media3.common.MediaItem.SubtitleConfiguration
import com.flixclusive.core.strings.Language
import com.flixclusive.core.ui.player.util.MimeTypeParser.toMimeType
import com.flixclusive.model.provider.link.Subtitle
import com.flixclusive.model.provider.link.SubtitleSource

// TODO: Test this function
internal fun getSubtitleSource(url: String): SubtitleSource {
    return when {
        url.contains("file://") -> SubtitleSource.LOCAL
        URLUtil.isValidUrl(url) -> SubtitleSource.ONLINE
        else -> SubtitleSource.EMBEDDED
    }
}

internal fun String.toNumberredSuffix(currentList: List<Subtitle>): String {
    var language = this
    var count = 0
    while (currentList.fastAny { it.language.equals(language, true) }) {
        count++
        language = "$this $count"
    }

    return language
}

internal fun <T> List<T>.getIndexOfPreferredLanguage(
    preferredLanguage: String,
    languageExtractor: (T) -> String,
): Int {
    val normalizedPreferredLanguage = normalizeLanguageCode(preferredLanguage)

    val index =
        indexOfFirst { item ->
            val extractedLanguage = languageExtractor(item)
            val normalizedExtractedLanguage = normalizeLanguageCode(extractedLanguage)

            normalizedExtractedLanguage == normalizedPreferredLanguage ||
                Language.map[normalizedExtractedLanguage]?.second?.equals(
                    Language.map[normalizedPreferredLanguage]?.second,
                    ignoreCase = true,
                ) == true
        }

    return maxOf(index, 0)
}

private fun normalizeLanguageCode(code: String): String {
    // Remove any non-alphanumeric characters and trim
    val cleanedCode = code.replace(Regex("[^A-Za-z0-9]"), " ").trim()

    // Split the cleaned string into words
    val words = cleanedCode.split("\\s+".toRegex())

    // Check each word (and combinations for multi-word languages)
    for (i in words.indices) {
        for (j in i until words.size) {
            val substring =
                words
                    .subList(i, j + 1)
                    .joinToString(" ")

            // Check if it's a 2-letter code
            if (substring.length == 2) {
                Language.map.entries
                    .find {
                        it.value.first.equals(substring, ignoreCase = true)
                    }?.key
                    ?.let { return it }
            }

            // Check if it's a 3-letter code
            if (substring.length == 3) {
                Language.map.entries
                    .find { it.key.equals(substring, ignoreCase = true) }
                    ?.key
                    ?.let { return it }
            }

            // Check if it matches a full language name
            Language.map.entries
                .find { it.value.second.equals(substring, ignoreCase = true) }
                ?.key
                ?.let { return it }

            // Check for partial matches (e.g., "Eng" for "English")
            Language.map.entries
                .find {
                    it.value.second.startsWith(
                        substring,
                        ignoreCase = true,
                    )
                }?.key
                ?.let { return it }
        }
    }

    // If no match found, return the original cleaned code
    return cleanedCode
}

internal fun List<Subtitle>.toSubtitleConfigurations(currentList: List<Subtitle>): List<SubtitleConfiguration> {
    if (isEmpty()) {
        return emptyList()
    }

    val sortedSubtitles =
        sortedWith(
            compareBy<Subtitle> { it.language.lowercase() }
                .thenBy {
                    it.language
                        .first()
                        .isLetterOrDigit()
                        .not()
                },
        )

    return sortedSubtitles
        .fastMapNotNull { subtitle ->
            val subtitleName = subtitle.language.toNumberredSuffix(currentList = currentList)
            val uri = Uri.parse(subtitle.url)
            val mimeType = subtitle.toMimeType()

            SubtitleConfiguration.Builder(uri)
                .apply {
                    setId(uri.toString())
                    setMimeType(mimeType)
                    setLabel(subtitleName)
                }.build()
        }
}
