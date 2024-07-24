package com.flixclusive.model.provider

import java.io.Serializable

/**
 *
 * A wrapper for the watchable url link of the extracted film to watch.
 *
 * @property url The url that links to the stream
 * @property name The detailed name of the stream link.
 * @property description The detailed description of the stream. Default is null
 * @property flags A set of resource [Flag]s associated with the subtitle. Default is null
 *
 * */
data class Stream(
    override val name: String,
    override val url: String,
    override val description: String? = null,
    override val flags: Set<Flag>? = null,
) : Serializable, MediaLink() {
    override fun equals(other: Any?): Boolean {
        val otherData = other as? Stream

        return name.equals(otherData?.name, true)
            && url == otherData?.url
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + url.hashCode()
        return result
    }
}