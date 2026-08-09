package com.flixclusive.feature.mobile.media.modal.stream

import android.net.Uri
import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.core.database.entity.downloads.DownloadItemState
import com.flixclusive.domain.downloads.usecase.CompletedDownloadFile
import com.flixclusive.model.media.common.MediaType
import com.flixclusive.model.media.common.tv.Episode
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import java.util.Date

class LocalPlaybackLinkTest {
    private fun episode(season: Int, number: Int) = Episode(
        id = "e-$season-$number",
        number = number,
        season = season,
        isReleased = true,
        title = "Episode $number",
    )

    @Test
    fun `an explicitly chosen episode is the only thing looked for`() {
        // The per-episode tap on MediaScreen says exactly what to play; nothing should second-guess it.
        val targets = localPlaybackTargets(
            isShow = true,
            navEpisode = episode(season = 3, number = 7),
            progressSeason = 1,
            progressEpisode = 1,
            isProgressCompleted = false,
        )

        expectThat(targets).isEqualTo(listOf(LocalPlaybackTarget(3, 7)))
    }

    @Test
    fun `a movie is looked for without a season or episode`() {
        val targets = localPlaybackTargets(
            isShow = false,
            navEpisode = null,
            progressSeason = null,
            progressEpisode = null,
            isProgressCompleted = false,
        )

        expectThat(targets).isEqualTo(listOf(LocalPlaybackTarget(null, null)))
    }

    @Test
    fun `a part-watched episode is resumed rather than rolled forward`() {
        val targets = localPlaybackTargets(
            isShow = true,
            navEpisode = null,
            progressSeason = 2,
            progressEpisode = 4,
            isProgressCompleted = false,
        )

        expectThat(targets).isEqualTo(listOf(LocalPlaybackTarget(2, 4)))
    }

    @Test
    fun `a finished episode rolls forward, then across the season, then replays itself`() {
        // Offline stand-in for the provider-backed next-episode lookup: whichever of these was
        // actually downloaded wins, and replaying beats refusing to play anything.
        val targets = localPlaybackTargets(
            isShow = true,
            navEpisode = null,
            progressSeason = 2,
            progressEpisode = 4,
            isProgressCompleted = true,
        )

        expectThat(targets).isEqualTo(
            listOf(
                LocalPlaybackTarget(2, 5),
                LocalPlaybackTarget(3, 1),
                LocalPlaybackTarget(2, 4),
            ),
        )
    }

    @Test
    fun `a show that was never opened starts at the first episode`() {
        val targets = localPlaybackTargets(
            isShow = true,
            navEpisode = null,
            progressSeason = null,
            progressEpisode = null,
            isProgressCompleted = false,
        )

        expectThat(targets).isEqualTo(listOf(LocalPlaybackTarget(1, 1)))
    }

    @Test
    fun `a downloaded file becomes a valid link the sheet can tell apart and play`() {
        val savedAt = Date(1_283_731_200_000L)
        val item = DownloadItem(
            id = "item-1",
            ownerId = "owner-1",
            mediaId = "m1",
            mediaTitle = "Show",
            mediaType = MediaType.SHOW,
            seasonNumber = 2,
            episodeNumber = 5,
            state = DownloadItemState.COMPLETED,
            updatedAt = savedAt,
        )
        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://downloads/video.mp4"

        val stream = item.toLocalCachedStream(
            file = CompletedDownloadFile(uri = uri, mimeType = "video/mp4"),
            ownerId = "owner-1",
            label = "Downloaded on this device",
        )

        expectThat(stream.url).isEqualTo("content://downloads/video.mp4")
        expectThat(stream.seasonNumber).isEqualTo(2)
        expectThat(stream.episodeNumber).isEqualTo(5)
        // Has to survive the sheet's `if (hasValidLinks)` gate, or it never renders.
        expectThat(stream.isValid).isTrue()
        expectThat(stream.isLocalDownload).isTrue()
        // Stable across re-emissions: the sheet keys its list rows on the link's hash.
        expectThat(stream.createdAt).isEqualTo(savedAt)
        expectThat(stream.updatedAt).isEqualTo(savedAt)
    }
}
