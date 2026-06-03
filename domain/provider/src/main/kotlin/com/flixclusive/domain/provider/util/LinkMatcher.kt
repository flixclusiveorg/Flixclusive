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

        return entries.firstNotNullOfOrNull { quality ->
            val index = indexOfFirst {
                quality.regex.match(it)
            }

            if (index != -1) index else null
        } ?: 0
    }
}
