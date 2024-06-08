package com.flixclusive.model.provider

import java.io.Serializable

/**
 *
 * A wrapper for the watchable url link of the extracted film to watch.
 *
 * @property name the name of the source where the provider got it from
 * @property url the url of the watchable source
 * @property customHeaders the custom headers of this link. some providers/extractors require custom authorized headers.
 *
 * */
data class SourceLink(
    val name: String,
    val url: String,
    val customHeaders: Map<String, String>? = null
) : Serializable {
    override fun equals(other: Any?): Boolean {
        val otherData = other as? SourceLink
        return name == otherData?.name && url == otherData.url
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + url.hashCode()
        return result
    }
}