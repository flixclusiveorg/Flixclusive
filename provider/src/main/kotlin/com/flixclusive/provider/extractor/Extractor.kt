package com.flixclusive.provider.extractor

import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import java.net.URL

/**
 * An extractor class for providers that contains embeds.
 */
abstract class Extractor {
    /**
     * The name of the extractor.
     */
    abstract val name: String

    /**
     * Alternate names for the extractor. Defaults to an empty list.
     */
    open val alternateNames: List<String> = emptyList()

    /**
     * The host associated with the extractor.
     */
    abstract val host: String

    /**
     * Extracts source links and subtitles from the provided URL.
     * @param url The URL to extract from.
     * @param mediaId The ID of the media.
     * @param episodeId The ID of the episode.
     * @param onLinkLoaded A callback function invoked when a source link is loaded.
     * @param onSubtitleLoaded A callback function invoked when a subtitle is loaded.
     */
    abstract suspend fun extract(
        url: URL,
        mediaId: String,
        episodeId: String,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit,
    )
}
