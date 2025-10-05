package com.flixclusive.core.presentation.player.util

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import java.util.Locale

internal object TracksUtil {
    /**
     * Gets all supported formats in a list
     *
     * @return List of [Format] objects representing the supported formats in all track groups.
     * */
    fun List<Tracks.Group>.getFormats() = map { it.getFormats() }.flatten()

    /**
     * Gets all supported formats in a list
     *
     * @return List of [Format] objects representing the supported formats in the track group.
     * */
    @OptIn(UnstableApi::class)
    fun Tracks.Group.getFormats(): List<Format> =
        (0 until this.mediaTrackGroup.length).mapNotNull { i ->
            mediaTrackGroup.getFormat(i)
        }

    /**
     * Returns a user-friendly name for the track based on its label and language.
     *
     * - If the label is available, it uses that as the primary name.
     * - If the label is not available, it defaults to "Subtitle Track #X" or "Audio Track #X" based on the track type.
     * - If the language is available and not "und" (undefined), it appends the language's display name.
     *
     * @param trackType The type of the track (e.g., C.TRACK_TYPE_TEXT for subtitles, C.TRACK_TYPE_AUDIO for audio).
     * @param index The index of the track, used for numbering when label is not available.
     *
     * @return A user-friendly name for the track.
     * */
    @UnstableApi
    fun Format.getName(
        trackType: @C.TrackType Int,
        index: Int,
    ): String {
        val language = language
        val label = label
        return buildString {
            if (label != null) {
                append(label)
            }

            if (isEmpty()) {
                if (trackType == C.TRACK_TYPE_TEXT) {
                    append("Subtitle Track #${index + 1}")
                } else {
                    append("Audio Track #${index + 1}")
                }
            }

            if (language != null && language != "und" && label == null) {
                append(": ")
                val locale = Locale.Builder().setLanguage(language).build()
                append(locale.displayLanguage)
            }
        }
    }

    /**
     * Finds the index of the item in the list that matches the preferred language.
     * The matching is done by comparing the normalized language codes or their display names.
     * If no match is found, it returns 0.
     *
     * @param preferredLanguage The preferred language code or name to match against.
     * @param languageProvider A lambda function that extracts the language code or name from an item of type T.
     *
     * @return The index of the item that matches the preferred language, or 0 if no match is found.
     * */
    fun <T> getIndexOfPreferredLanguage(
        list: List<T>,
        preferredLanguage: String,
        languageProvider: (T) -> String,
    ): Int {
        val normalizedPreferredLanguage = normalizeLanguageCode(preferredLanguage)

        val index =
            list.indexOfFirst { item ->
                val extractedLanguage = languageProvider(item)
                val normalizedExtractedLanguage = normalizeLanguageCode(extractedLanguage)

                val extractedLanguageDisplayName = Language.map[normalizedExtractedLanguage]?.second
                val preferredLanguageDisplayName = Language.map[normalizedPreferredLanguage]?.second

                normalizedExtractedLanguage == normalizedPreferredLanguage ||
                    (
                        extractedLanguageDisplayName != null &&
                        preferredLanguageDisplayName != null &&
                        (
                            extractedLanguageDisplayName.contains(preferredLanguageDisplayName, ignoreCase = true) ||
                            extractedLanguageDisplayName.equals(preferredLanguageDisplayName, ignoreCase = true)
                            )
                    )
            }

        return maxOf(index, 0)
    }

    /**
     * Normalizes a language code by removing non-alphanumeric characters, trimming whitespace,
     * and checking against known language codes and names.
     * It supports 2-letter and 3-letter ISO codes, as well as full and partial language names.
     *
     * @param code The language code or name to normalize.
     *
     * @return The normalized language code if found in the known languages, otherwise returns the cleaned input code.
     * */
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
}
