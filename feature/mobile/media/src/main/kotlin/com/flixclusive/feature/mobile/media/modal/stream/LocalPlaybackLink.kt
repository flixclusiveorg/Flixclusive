package com.flixclusive.feature.mobile.media.modal.stream

import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.core.database.entity.provider.CachedMediaLink
import com.flixclusive.core.database.entity.provider.CachedStream
import com.flixclusive.domain.downloads.usecase.CompletedDownloadFile
import com.flixclusive.model.media.common.tv.Episode

/**
 * Stands in for a provider id on the synthesized link for a downloaded file, so the sheet can tell
 * the one link it must play through [com.flixclusive.core.navigation.navargs.PlaybackRequest.FromDownload]
 * apart from the provider links around it.
 *
 * Not a real provider, and never written to the database: the link cache is also what the
 * downloader ranks candidates from, and a `content://` row there would have it try to download a
 * file that is already on disk.
 */
internal const val LOCAL_DOWNLOAD_PROVIDER_ID = "flixclusive.local-download"

internal val CachedMediaLink.isLocalDownload: Boolean
    get() = providerId == LOCAL_DOWNLOAD_PROVIDER_ID

/** A finished download offered as a playable link, paired with the id playback needs. */
internal data class LocalLink(
    val downloadItemId: String,
    val stream: CachedStream,
)

/** One media/episode to look for a finished download of. Both numbers null for a movie. */
internal data class LocalPlaybackTarget(
    val seasonNumber: Int?,
    val episodeNumber: Int?,
)

/**
 * The episodes worth checking for a download, best candidate first.
 *
 * Derived from nothing but the nav arg and stored watch progress on purpose. The sheet's own
 * episode resolution reaches a provider to expand a partial season and fetches metadata over the
 * network for the `PartialMedia` that Continue Watching passes — neither of which is available to
 * someone playing a download with no connection, which is the whole point of having one.
 *
 * A finished episode rolls forward the way the provider-backed next-episode lookup would: the next
 * episode, then the next season's first, falling back to replaying the episode itself.
 */
internal fun localPlaybackTargets(
    isShow: Boolean,
    navEpisode: Episode?,
    progressSeason: Int?,
    progressEpisode: Int?,
    isProgressCompleted: Boolean,
): List<LocalPlaybackTarget> {
    if (navEpisode != null) {
        return listOf(LocalPlaybackTarget(navEpisode.season, navEpisode.number))
    }

    if (!isShow) return listOf(LocalPlaybackTarget(null, null))

    // The `?: 1` defaults match what the sheet falls back to for a show that was never opened.
    val season = progressSeason ?: 1
    val episode = progressEpisode ?: 1

    if (!isProgressCompleted) return listOf(LocalPlaybackTarget(season, episode))

    return listOf(
        LocalPlaybackTarget(season, episode + 1),
        LocalPlaybackTarget(season + 1, 1),
        LocalPlaybackTarget(season, episode),
    )
}

/**
 * Dresses a finished download as the link the sheet renders.
 *
 * [DownloadItem.updatedAt] seeds both timestamps rather than `Date()`, because the sheet keys its
 * list on each link's hash: a timestamp that moved on every re-emission would hand the row a new
 * identity and make it churn.
 */
internal fun DownloadItem.toLocalCachedStream(
    file: CompletedDownloadFile,
    ownerId: String,
    label: String,
): CachedStream = CachedStream(
    url = file.uri.toString(),
    label = label,
    providerId = LOCAL_DOWNLOAD_PROVIDER_ID,
    ownerId = ownerId,
    mediaId = mediaId,
    seasonNumber = seasonNumber,
    episodeNumber = episodeNumber,
    createdAt = updatedAt,
    updatedAt = updatedAt,
)
