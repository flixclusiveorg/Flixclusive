package com.flixclusive.feature.mobile.settings.screen.downloads

import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.model.media.common.MediaType
import java.util.Date

internal sealed class DownloadListEntry {
    /**
     * What the list is ordered by. Deliberately [DownloadItem.createdAt] and not
     * [DownloadItem.updatedAt]: `updatedAt` is rewritten on every throttled progress write, about
     * once a second per active transfer, so ordering by it made rows leapfrog each other while
     * several downloads ran at once. When an item was queued never changes, so the list holds
     * still.
     */
    abstract val sortedAt: Date

    data class Single(
        val item: DownloadItem,
    ) : DownloadListEntry() {
        override val sortedAt: Date get() = item.createdAt
    }

    data class Batch(
        val mediaId: String,
        val seasonNumber: Int,
        val mediaTitle: String,
        val items: List<DownloadItem>,
    ) : DownloadListEntry() {
        /** The earliest of the group, so queueing another episode into an existing season doesn't
         * shunt the whole batch to the top. */
        override val sortedAt: Date get() = items.minOf { it.createdAt }
    }
}

/**
 * Collapses [items] into the rows the downloads screen renders: one entry per show season, one per
 * standalone title, newest queued first.
 */
internal fun groupIntoEntries(items: List<DownloadItem>): List<DownloadListEntry> {
    val (groupable, standalone) = items.partition {
        it.mediaType == MediaType.SHOW && it.seasonNumber != null
    }

    val batches = groupable
        .groupBy { it.mediaId to it.seasonNumber }
        .map { (key, groupItems) ->
            DownloadListEntry.Batch(
                mediaId = key.first,
                seasonNumber = key.second!!,
                mediaTitle = groupItems.first().mediaTitle,
                items = groupItems.sortedBy { it.episodeNumber ?: 0 },
            )
        }

    val singles = standalone.map { DownloadListEntry.Single(it) }

    // Ordered by DownloadListEntry.sortedAt, not by updatedAt: see the doc on that property for
    // why sorting on progress writes made the list shuffle while downloads ran.
    return (batches + singles).sortedByDescending { it.sortedAt }
}
