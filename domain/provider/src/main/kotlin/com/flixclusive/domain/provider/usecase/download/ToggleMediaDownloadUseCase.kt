package com.flixclusive.domain.provider.usecase.download

import com.flixclusive.core.common.domain.Async
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.Show
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.model.media.common.tv.Season

/** What a toggle applies to. The behaviour is the same operation at two different scopes. */
sealed interface DownloadTarget {
    /** A movie, or one episode when [episode] is given. */
    data class Single(
        val media: MediaMetadata,
        val episode: Episode? = null,
    ) : DownloadTarget

    /** Every not-yet-downloaded episode of [season]. */
    data class WholeSeason(
        val show: Show,
        val season: Season.Full,
    ) : DownloadTarget
}

/**
 * Starts, stops or retries the download for [DownloadTarget], depending on what already exists.
 *
 * Lives in `domain/provider` rather than `domain/downloads` because it needs
 * [com.flixclusive.domain.provider.usecase.get.GetMediaLinksUseCase] to warm the link cache before
 * queueing, and `domain/provider` already depends on `domain/downloads` — the reverse edge would
 * close a cycle.
 *
 * Returns only the outcome. Whatever per-target progress the caller shows while this runs is UI
 * state and stays with the caller.
 */
interface ToggleMediaDownloadUseCase {
    suspend operator fun invoke(target: DownloadTarget): Async<Unit>
}
