package com.flixclusive.feature.mobile.settings.screen.downloads

import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.model.media.common.MediaType
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import java.util.Date

class DownloadListEntryTest {
    private fun item(
        id: String,
        createdAtMs: Long,
        updatedAtMs: Long = createdAtMs,
        mediaId: String = id,
        mediaType: MediaType = MediaType.MOVIE,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null,
    ) = DownloadItem(
        id = id,
        ownerId = "owner-1",
        mediaId = mediaId,
        mediaTitle = "Title $mediaId",
        mediaType = mediaType,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
        createdAt = Date(createdAtMs),
        updatedAt = Date(updatedAtMs),
    )

    private fun idsOf(entries: List<DownloadListEntry>) = entries.map { entry ->
        when (entry) {
            is DownloadListEntry.Single -> entry.item.id
            is DownloadListEntry.Batch -> "${entry.mediaId}-s${entry.seasonNumber}"
        }
    }

    @Test
    fun `should order newest queued first`() {
        val entries = groupIntoEntries(
            listOf(
                item(id = "oldest", createdAtMs = 1_000),
                item(id = "newest", createdAtMs = 3_000),
                item(id = "middle", createdAtMs = 2_000),
            )
        )

        expectThat(idsOf(entries)).containsExactly("newest", "middle", "oldest")
    }

    @Test
    fun `should keep the same order when a download reports progress`() {
        // The regression: updatedAt is rewritten roughly once a second per active transfer, so
        // ordering by it made whichever item reported last jump the queue, and the list visibly
        // re-sorted underneath the user while several downloads ran.
        val items = listOf(
            item(id = "first", createdAtMs = 3_000),
            item(id = "second", createdAtMs = 2_000),
            item(id = "third", createdAtMs = 1_000),
        )
        val before = idsOf(groupIntoEntries(items))

        val afterProgress = items.map {
            if (it.id == "third") it.copy(updatedAt = Date(9_999)) else it
        }

        expectThat(idsOf(groupIntoEntries(afterProgress))).isEqualTo(before)
    }

    @Test
    fun `should group a show's season into one batch ordered by episode`() {
        val entries = groupIntoEntries(
            listOf(
                item("s1e2", 1_000, mediaId = "show", mediaType = MediaType.SHOW, seasonNumber = 1, episodeNumber = 2),
                item("s1e1", 1_000, mediaId = "show", mediaType = MediaType.SHOW, seasonNumber = 1, episodeNumber = 1),
            )
        )

        expectThat(entries).hasSize(1)
        val batch = entries.first()
        expectThat(batch).isA<DownloadListEntry.Batch>()
        expectThat((batch as DownloadListEntry.Batch).items.map { it.id }).containsExactly("s1e1", "s1e2")
    }

    @Test
    fun `should not move a batch when another episode is queued into it later`() {
        // Batches sort on their earliest member, so adding episode 2 an hour later leaves the
        // season sitting where the user last saw it rather than yanking it to the top.
        val movie = item(id = "movie", createdAtMs = 5_000)
        val episode1 =
            item("s1e1", 1_000, mediaId = "show", mediaType = MediaType.SHOW, seasonNumber = 1, episodeNumber = 1)
        val episode2 =
            item("s1e2", 9_000, mediaId = "show", mediaType = MediaType.SHOW, seasonNumber = 1, episodeNumber = 2)

        expectThat(idsOf(groupIntoEntries(listOf(movie, episode1, episode2))))
            .containsExactly("movie", "show-s1")
    }
}
