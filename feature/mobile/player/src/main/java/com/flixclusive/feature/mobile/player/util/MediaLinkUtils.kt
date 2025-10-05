package com.flixclusive.feature.mobile.player.util

import androidx.compose.ui.util.fastMapIndexed
import com.flixclusive.core.presentation.player.model.track.MediaTrack
import com.flixclusive.model.provider.link.MediaLink

internal object MediaLinkUtils {
    /**
     * Cleans duplicate names in a list of [MediaLink] objects and maps them to [MediaTrack] objects.
     *
     * If multiple items have the same name, they are suffixed with a number to differentiate them (e.g., "English", "English 2").
     *
     * @return List of [MediaTrack] objects.
     * */
    fun <T : MediaTrack> List<MediaLink>.cleanDuplicates(
        transform: (Int, String) -> T
    ): List<T> {
        // Create a map to track the occurrence of each name
        val names = mutableMapOf<String, Int>()

        return fastMapIndexed { index, media ->
            val count = names[media.name] ?: 1
            val label = if (count > 1) {
                "${media.name} $count"
            } else {
                media.name
            }

            names[media.name] = count + 1

            transform(index, label)
        }
    }
}
