package com.flixclusive.core.ui.player.util

import android.content.Context
import com.flixclusive.core.locale.Language
import com.flixclusive.model.provider.link.Subtitle
import com.flixclusive.model.provider.link.SubtitleSource
import com.flixclusive.core.locale.R as LocaleR

@Suppress("MemberVisibilityCanBePrivate")
object PlayerTracksHelper {
    internal fun <T> List<T>.getIndexOfPreferredLanguage(
        preferredLanguage: String,
        languageExtractor: (T) -> String
    ): Int {
        val normalizedPreferredLanguage = normalizeLanguageCode(preferredLanguage)

        val index = indexOfFirst { item ->
            val extractedLanguage = languageExtractor(item)
            val normalizedExtractedLanguage = normalizeLanguageCode(extractedLanguage)

            normalizedExtractedLanguage == normalizedPreferredLanguage ||
                    Language.map[normalizedExtractedLanguage]?.second?.equals(
                        Language.map[normalizedPreferredLanguage]?.second, ignoreCase = true) == true
        }

        return maxOf(index, 0)
    }

    /**
     * Initializes the subtitles by adding an "Off" option and updating the video data with the new subtitles.
     */
    internal fun List<Subtitle>.addOffSubtitle(
        context: Context
    ): List<Subtitle> {
        return listOf(
            Subtitle(
                url = "",
                language = context.getString(LocaleR.string.off_subtitles),
                type = SubtitleSource.EMBEDDED
            )
        ) + this
    }

    fun normalizeLanguageCode(code: String): String {
        // Remove any non-alphanumeric characters and trim
        val cleanedCode = code.replace(Regex("[^A-Za-z0-9]"), " ").trim()

        // Split the cleaned string into words
        val words = cleanedCode.split("\\s+".toRegex())

        // Check each word (and combinations for multi-word languages)
        for (i in words.indices) {
            for (j in i until words.size) {
                val substring = words.subList(i, j + 1)
                    .joinToString(" ")

                // Check if it's a 2-letter code
                if (substring.length == 2) {
                    Language.map.entries.find {
                        it.value.first.equals(substring, ignoreCase = true)
                    }?.key?.let { return it }
                }

                // Check if it's a 3-letter code
                if (substring.length == 3) {
                    Language.map.entries.find { it.key.equals(substring, ignoreCase = true) }?.key?.let { return it }
                }

                // Check if it matches a full language name
                Language.map.entries.find { it.value.second.equals(substring, ignoreCase = true) }?.key?.let { return it }

                // Check for partial matches (e.g., "Eng" for "English")
                Language.map.entries.find { it.value.second.startsWith(substring, ignoreCase = true) }?.key?.let { return it }
            }
        }

        // If no match found, return the original cleaned code
        return cleanedCode
    }
}
