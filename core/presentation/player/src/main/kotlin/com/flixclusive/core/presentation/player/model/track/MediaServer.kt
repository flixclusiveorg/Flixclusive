package com.flixclusive.core.presentation.player.model.track

import androidx.compose.runtime.Immutable
import com.flixclusive.core.datastore.model.user.player.PlayerQuality
import com.flixclusive.core.datastore.model.user.player.PlayerQuality.entries

/**
 * Immutable data class representing available servers.
 * This is exposed to the UI layer for server selection.
 */
@Immutable
data class MediaServer(
    override val label: String,
    val url: String,
    val headers: Map<String, String>?,
    val source: TrackSource,
) : MediaTrack {
    companion object {
        fun List<MediaServer>.getIndexOfPreferredQuality(preference: PlayerQuality): Int {
            val preferredQualityIndex = indexOfFirst {
                preference.regex.containsMatchIn(it)
            }

            if (preferredQualityIndex != -1) {
                return preferredQualityIndex
            }

            return entries.firstNotNullOfOrNull { quality ->
                val index = indexOfFirst {
                    quality.regex.containsMatchIn(it)
                }

                if (index != -1) index else null
            } ?: 0
        }

        private fun Regex.containsMatchIn(link: MediaServer): Boolean {
            return containsMatchIn(link.label) || containsMatchIn(link.url)
        }
    }
}
