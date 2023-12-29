package com.flixclusive.providers.models.common

import java.io.Serializable

data class SourceLink(
    val name: String,
    val url: String,
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