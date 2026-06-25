package com.flixclusive.feature.mobile.media.util

import com.flixclusive.feature.mobile.media.ContentTabType
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.Movie

internal object MediaScreenUtils {
    fun getTabs(media: MediaMetadata): List<ContentTabType> {
        val tabs = mutableListOf<ContentTabType>()

        if (media.isShow) {
            tabs.add(ContentTabType.Episodes)
        }

        if (media.recommendations.isNotEmpty()) {
            tabs.add(ContentTabType.MoreLikeThis)
        }

        if (media is Movie && media.collection?.parts?.isNotEmpty() == true) {
            tabs.add(ContentTabType.Collections)
        }

        return tabs.toList()
    }
}
