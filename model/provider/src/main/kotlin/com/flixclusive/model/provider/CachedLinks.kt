package com.flixclusive.model.provider

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
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
    fun add(link: MediaLink) {
        when {
            link is Stream && !streams.contains(link) -> {
                streams.add(link)
            }
            link is Subtitle && !subtitles.contains(link) -> {
                subtitles.add(link)
            }
        }
    }
}