package com.flixclusive.extractor.base

import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import java.net.URL

/**
 *
 * An extractor class for providers that contains embeds
 * */
abstract class Extractor {
    abstract val name: String
    abstract val host: String

    abstract suspend fun extract(
        url: URL,
        mediaId: String,
        episodeId: String,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit,
    )
}