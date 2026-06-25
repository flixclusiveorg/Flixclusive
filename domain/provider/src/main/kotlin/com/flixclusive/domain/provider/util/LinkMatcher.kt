package com.flixclusive.domain.provider.util

import com.flixclusive.core.datastore.model.user.player.PlayerQuality
import com.flixclusive.core.datastore.model.user.player.PlayerQuality.entries

object LinkMatcher {
    fun <T> List<T>.getIndexOfPreferredQuality(
        preference: PlayerQuality,
        match: Regex.(T) -> Boolean
    ): Int {
        val preferredQualityIndex = indexOfFirst {
            preference.regex.match(it)
        }

        if (preferredQualityIndex != -1) {
            return preferredQualityIndex
        }

        // If the preferred quality is not found, check for other qualities in order of preference
        // We start from the next quality after the preferred one, and loop through the qualities in a circular manner
        for (i in 1 until entries.size) {
            val nextQuality = entries[(preference.ordinal + i) % entries.size]
            val index = indexOfFirst {
                nextQuality.regex.match(it)
            }

            if (index != -1) {
                return index
            }
        }

        return 0 // Default to the first link if no matches are found
    }
}
