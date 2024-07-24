package com.flixclusive.provider.extractor

import com.flixclusive.model.provider.MediaLink
import okhttp3.OkHttpClient

/**
 * An extractor class for providers that uses indirect and implicit link data sources.
 *
 * @param client The [OkHttpClient] instance used for network requests.
 *
 * @property name The name of the extractor.
 * @property baseUrl The base URL associated with the extractor.
 */
@Suppress("unused")
abstract class Extractor(
    protected val client: OkHttpClient
) {
    abstract val name: String
    abstract val baseUrl: String

    /**
     * Extracts resource links from the provided embed URL.
     *
     * @param url The URL to extract from.
     * @param customHeaders Additional headers to include in the request.
     *
     * @return A list of [MediaLink] objects representing the extracted links.
     */
    abstract suspend fun extract(
        url: String,
        customHeaders: Map<String, String>? = null
    ): List<MediaLink>
}
