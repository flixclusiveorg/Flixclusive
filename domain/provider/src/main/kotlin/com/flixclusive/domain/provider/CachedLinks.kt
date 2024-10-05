package com.flixclusive.domain.provider

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.util.fastAny
import com.flixclusive.model.provider.link.MediaLink
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle
import java.io.Serializable

/**
 *
 * A data model to hold all cached [MediaLink]s that was extracted from a provider.
 *
 * @param watchId the watch id of the film from the used provider
 * @param providerName the name of the provider used
 * @param streams watchable links obtained from the provider
 * @param subtitles subtitle links obtained from the provider
 *
 * @see MediaLink
 * @see Stream
 * @see Subtitle
 * */
data class CachedLinks(
    val watchId: String = "",
    val providerName: String = "",
    val streams: SnapshotStateList<Stream> = mutableStateListOf(),
    val subtitles: SnapshotStateList<Subtitle> = mutableStateListOf()
) : Serializable {
    private fun containsStream(link: Stream): Boolean {
        return streams.fastAny {
            link.url.equals(it.url, true)
        }
    }

    private fun containsSubtitle(link: Subtitle): Boolean {
        return subtitles.fastAny {
            link.url.equals(it.url, true)
        }
    }

    fun add(link: MediaLink) {
        when {
            link is Stream && !containsStream(link) -> {
                streams.add(link)
            }
            link is Subtitle && !containsSubtitle(link) -> {
                subtitles.add(link)
            }
        }
    }
}