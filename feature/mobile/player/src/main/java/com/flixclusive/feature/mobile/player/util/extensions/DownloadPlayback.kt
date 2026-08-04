package com.flixclusive.feature.mobile.player.util.extensions

import androidx.compose.ui.util.fastMap
import androidx.media3.common.MimeTypes
import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.core.presentation.player.model.track.PlayerServer
import com.flixclusive.core.presentation.player.model.track.PlayerSubtitle
import com.flixclusive.core.presentation.player.model.track.TrackSource
import com.flixclusive.domain.downloads.usecase.CompletedDownloadFile
import com.flixclusive.domain.downloads.usecase.CompletedSubtitleFile
import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.common.tv.Episode

/** Maps a resolved local file to the single [PlayerServer] local playback offers — there is no
 * server/provider choice for a downloaded file, only the file itself. */
internal fun CompletedDownloadFile.toPlayerServer(label: String): PlayerServer = PlayerServer(
    label = label,
    url = uri.toString(),
    isDead = false,
    headers = null,
    source = TrackSource.LOCAL,
)

/** Maps a resolved local file's sibling subtitles to [PlayerSubtitle]s. [CompletedSubtitleFile.extension]
 * carries the real on-disk MIME hint — see [CompletedDownloadFile]'s KDoc for why this beats guessing
 * from the label. */
internal fun CompletedDownloadFile.toPlayerSubtitles(): List<PlayerSubtitle> = subtitles.fastMap { subtitle ->
    PlayerSubtitle(
        label = subtitle.language,
        url = subtitle.uri.toString(),
        isDead = false,
        source = TrackSource.LOCAL,
        mimeType = subtitle.extension.toSubtitleMimeType(),
    )
}

private fun String.toSubtitleMimeType(): String = when (lowercase()) {
    "vtt" -> MimeTypes.TEXT_VTT
    "ssa", "ass" -> MimeTypes.TEXT_SSA
    "ttml", "xml" -> MimeTypes.APPLICATION_TTML
    else -> MimeTypes.APPLICATION_SUBRIP
}

/** Builds the [Episode] a downloaded episode's [DownloadItem] represents, or null for a movie
 * download. Relocated from the settings module's downloads screen — playback-facing episode
 * shape belongs to the player, not to the screen that merely opens it. */
internal fun DownloadItem.toEpisode(): Episode? {
    val season = seasonNumber ?: return null
    val episode = episodeNumber ?: return null

    return Episode(
        id = "$mediaId-$season-$episode",
        number = episode,
        season = season,
        isReleased = true,
        title = "S${season.toString().padStart(2, '0')}E${episode.toString().padStart(2, '0')}",
    )
}

/** Minimal stand-in [PartialMedia] used only when a completed download's `media` row is somehow
 * missing (see [com.flixclusive.data.provider.repository.MediaLinksRepository.getMedia]) — every
 * completed download is expected to have one, so this is belt-and-braces, not a supported mode. */
internal fun DownloadItem.toFallbackMedia(): PartialMedia = PartialMedia(
    type = mediaType,
    id = mediaId,
    title = mediaTitle,
    providerId = "",
    posterImage = null,
)
