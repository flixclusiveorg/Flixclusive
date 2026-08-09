package com.flixclusive.domain.downloads.usecase

import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.tv.Episode

/**
 * Queues a download and starts it, returning the id of the row it reports against.
 *
 * Starting is part of the contract rather than the caller's job: a queued row nobody dispatched is
 * dead weight, and leaving it optional is what let the single-item and batch paths drift apart —
 * the batch one started what it queued, this one silently did not.
 *
 * The id is not necessarily a new one. If this media is already queued, the existing row is
 * returned rather than a second being inserted for the same file on disk.
 */
interface QueueMediaDownloadUseCase {
    suspend operator fun invoke(
        media: MediaMetadata,
        episode: Episode?,
        ownerId: String,
    ): String
}
