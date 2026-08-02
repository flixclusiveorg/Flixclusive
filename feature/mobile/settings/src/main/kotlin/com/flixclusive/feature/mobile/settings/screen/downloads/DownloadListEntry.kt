package com.flixclusive.feature.mobile.settings.screen.downloads

import com.flixclusive.core.database.entity.downloads.DownloadItem
import java.util.Date

internal sealed class DownloadListEntry {
    abstract val latestUpdatedAt: Date

    data class Single(
        val item: DownloadItem,
    ) : DownloadListEntry() {
        override val latestUpdatedAt: Date get() = item.updatedAt
    }

    data class Batch(
        val mediaId: String,
        val seasonNumber: Int,
        val mediaTitle: String,
        val items: List<DownloadItem>,
    ) : DownloadListEntry() {
        override val latestUpdatedAt: Date get() = items.maxOf { it.updatedAt }
    }
}
