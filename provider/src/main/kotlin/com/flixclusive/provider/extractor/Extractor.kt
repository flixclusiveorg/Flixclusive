package com.flixclusive.provider.extractor

import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import okhttp3.OkHttpClient

/**
 * An extractor class for providers that contains iframe embeds.
 * @param client The [OkHttpClient] instance used for network requests.
 */
abstract class Extractor(
    protected val client: OkHttpClient
) {
    /**
     * The name of the extractor.
     */
    abstract val name: String

    /**
     * The base url associated with the extractor.
     */
    abstract val baseUrl: String

    /**
     * Extracts source links and subtitles from the provided URL.
     * @param url The URL to extract from.
     * @param onLinkLoaded A callback function invoked when a source link is loaded.
     * @param onSubtitleLoaded A callback function invoked when a subtitle is loaded.
     */
    abstract suspend fun extract(
        url: String,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit,
    )
}
