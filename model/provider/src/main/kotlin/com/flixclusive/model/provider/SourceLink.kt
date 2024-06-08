package com.flixclusive.model.provider

import java.io.Serializable

/**
 *
 * A wrapper for the watchable url link of the extracted film to watch.
 *
 * @param name the name of the source where the provider got it from
 * @param url the url of the watchable source
 * @param referer the referer url of the watchable source. some providers need it
 *
 * */
data class SourceLink(
    val name: String,
    val url: String,
    val referer: String? = null
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