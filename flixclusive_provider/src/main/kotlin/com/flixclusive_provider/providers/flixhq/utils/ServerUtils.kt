package com.flixclusive_provider.providers.flixhq.utils

import org.jsoup.select.Elements
import java.util.Locale

object ServerUtils {
    fun Elements.getServerName(mediaId: String): String {
        val anchorElement = select("a")
        val titleElement = anchorElement.attr("title")

        return if (mediaId.contains("movie")) {
            titleElement
                .lowercase(Locale.getDefault())
        } else {
            titleElement.substring(6)
                .trim()
                .lowercase(Locale.getDefault())
        }
    }

    fun Elements.getServerUrl(
        baseUrl: String,
        mediaId: String
    ): String {
        return "${baseUrl}/${mediaId}.${
            if (!mediaId.contains("movie")) {
                attr("data-id")
            } else {
                attr("data-linkid")
            }
        }".replace(
            if (!mediaId.contains("movie")) {
                Regex("/tv/")
            } else {
                Regex("/movie/")
            },
            if (!mediaId.contains("movie")) {
                "/watch-tv/"
            } else {
                "/watch-movie/"
            }
        )
    }
}