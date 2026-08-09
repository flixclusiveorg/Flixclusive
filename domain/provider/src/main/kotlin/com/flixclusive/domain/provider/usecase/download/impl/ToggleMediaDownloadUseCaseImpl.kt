package com.flixclusive.domain.provider.usecase.download.impl

import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.core.database.entity.downloads.DownloadItemState
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.downloads.repository.MediaDownloadRepository
import com.flixclusive.domain.downloads.controller.MediaDownloadController
import com.flixclusive.domain.downloads.model.MediaDownloadRequest
import com.flixclusive.domain.downloads.usecase.QueueMediaDownloadBatchUseCase
import com.flixclusive.domain.downloads.usecase.QueueMediaDownloadUseCase
import com.flixclusive.domain.provider.usecase.download.DownloadTarget
import com.flixclusive.domain.provider.usecase.download.ToggleMediaDownloadUseCase
import com.flixclusive.domain.provider.usecase.get.GetMediaLinksUseCase
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.tv.Episode
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import javax.inject.Inject

internal class ToggleMediaDownloadUseCaseImpl @Inject constructor(
    private val mediaDownloadRepository: MediaDownloadRepository,
    private val mediaDownloadController: MediaDownloadController,
    private val queueMediaDownload: QueueMediaDownloadUseCase,
    private val queueMediaDownloadBatch: QueueMediaDownloadBatchUseCase,
    private val getMediaLinks: GetMediaLinksUseCase,
    private val userSessionDataStore: UserSessionDataStore,
) : ToggleMediaDownloadUseCase {
    override suspend fun invoke(target: DownloadTarget): Async<Unit> =
        when (target) {
            is DownloadTarget.Single -> toggleSingle(target)
            is DownloadTarget.WholeSeason -> toggleSeason(target)
        }

    private suspend fun toggleSingle(target: DownloadTarget.Single): Async<Unit> {
        val existing = mediaDownloadRepository.getFor(
            mediaId = target.media.id,
            seasonNumber = target.episode?.season,
            episodeNumber = target.episode?.number,
        )

        return when {
            existing == null -> resolveAndQueue(target.media, target.episode)
            else -> Async.Success(existing.toggle())
        }
    }

    private suspend fun toggleSeason(target: DownloadTarget.WholeSeason): Async<Unit> {
        val (show, season) = target
        val existingBatch = mediaDownloadRepository.getBatch(show.id, season.number)

        if (existingBatch.any { !it.state.isTerminal }) {
            mediaDownloadController.stopBatch(show.id, season.number)
            return Async.Success(Unit)
        }

        val retryable = existingBatch.filter { it.state.isTerminal && it.state != DownloadItemState.COMPLETED }
        if (retryable.isNotEmpty()) {
            retryable.forEach { mediaDownloadController.retry(it.id) }
            return Async.Success(Unit)
        }

        val alreadyQueued = existingBatch.mapNotNullTo(mutableSetOf()) { it.episodeNumber }
        val episodesToQueue = season.episodes.filterNot { it.number in alreadyQueued }
        if (episodesToQueue.isEmpty()) return Async.Success(Unit)

        // Ensure every episode has links cached before queueing, since the download engine
        // only ever reads from the link cache and never invokes a provider itself. Resolved
        // sequentially since provider plugins are third-party code with no thread-safety
        // guarantee, and concurrent resolution would fan out unbounded parallel calls into
        // the same provider plugin instance across every episode in the season at once.
        episodesToQueue.forEach { episode -> ensureMediaLinksLoaded(show, episode) }

        val ownerId = currentOwnerId()
        queueMediaDownloadBatch(
            episodesToQueue.map { episode ->
                MediaDownloadRequest(media = show, episode = episode, ownerId = ownerId)
            },
        )

        return Async.Success(Unit)
    }

    /** Stops an in-flight download, retries a failed one, and leaves a finished one alone. */
    private fun DownloadItem.toggle() {
        when {
            !state.isTerminal -> mediaDownloadController.stop(id)
            state == DownloadItemState.COMPLETED -> Unit
            else -> mediaDownloadController.retry(id)
        }
    }

    private suspend fun resolveAndQueue(
        media: MediaMetadata,
        episode: Episode?,
    ): Async<Unit> {
        // The download engine only ever reads cached links, so trigger the same link-loading
        // path the Play button uses here to avoid requiring the user to open Play first.
        val links = ensureMediaLinksLoaded(media, episode)
        if (links is Async.Failure) return links

        queueMediaDownload(media, episode, currentOwnerId())
        return Async.Success(Unit)
    }

    private suspend fun ensureMediaLinksLoaded(
        media: MediaMetadata,
        episode: Episode?,
    ): Async<Unit> {
        val finalState = getMediaLinks(media, episode).last()
        return if (finalState.isSuccess) Async.Success(Unit) else Async.Failure(finalState.message)
    }

    private suspend fun currentOwnerId(): String = userSessionDataStore.currentUserId.filterNotNull().first()
}
