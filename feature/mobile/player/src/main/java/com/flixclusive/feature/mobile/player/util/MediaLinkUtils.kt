package com.flixclusive.feature.mobile.player.util

import androidx.compose.ui.util.fastMap
import com.flixclusive.core.presentation.player.model.track.PlayerServer
import com.flixclusive.core.presentation.player.model.track.PlayerSubtitle
import com.flixclusive.core.presentation.player.model.track.PlayerTrack
import com.flixclusive.core.presentation.player.model.track.TrackSource
import com.flixclusive.model.provider.link.MediaLink
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle

internal object MediaLinkUtils {
    /**
     * Cleans duplicate names in a list of [MediaLink] objects and maps them to [PlayerTrack] objects.
     *
     * If multiple items have the same name, they are suffixed with a number to differentiate them (e.g., "English", "English 2").
     *
     * @return List of [PlayerTrack] objects.
     * */
    fun <T : PlayerTrack> List<MediaLink>.cleanDuplicates(
        transform: (MediaLink) -> T
    ): List<T> {
        // Create a map to track the occurrence of each name
        val names = mutableMapOf<String, Int>()

        return fastMap { media ->
            val count = names[media.name] ?: 1
            val label = if (count > 1) {
                "${media.name} $count"
            } else {
                media.name
            }

            names[media.name] = count + 1

            val updatedMedia = if (media is Stream) {
                media.copy(name = label)
            } else if (media is Subtitle) {
                media.copy(language = label)
            } else {
                throw IllegalArgumentException("Unsupported media type: ${media::class}")
            }

            transform(updatedMedia)
        }
    }

    internal fun Stream.toPlayerServer(): PlayerServer {
        return PlayerServer(
            label = name,
            url = url,
            headers = customHeaders,
            source = TrackSource.REMOTE,
        )
    }

    internal fun Subtitle.toPlayerSubtitle(): PlayerSubtitle {
        return PlayerSubtitle(
            label = name,
            url = url,
            source = TrackSource.REMOTE,
        )
    }
}
