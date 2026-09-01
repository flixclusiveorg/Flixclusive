package com.flixclusive.domain.provider.usecase.tracker.impl

import com.flixclusive.core.util.coroutines.mapAsync
import com.flixclusive.data.provider.repository.TrackerListKey
import com.flixclusive.data.provider.repository.TrackerListRepository
import com.flixclusive.domain.provider.usecase.get.GetCrossMatchedMediaMetadataUseCase
import com.flixclusive.domain.provider.usecase.get.GetTrackerProvidersUseCase
import com.flixclusive.domain.provider.usecase.tracker.GetTrackerListsForMediaUseCase
import com.flixclusive.domain.provider.usecase.tracker.TrackerListAndProvider
import com.flixclusive.domain.provider.usecase.tracker.TrackerLists
import com.flixclusive.model.media.MediaMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class GetTrackerListsForMediaUseCaseImpl @Inject constructor(
    private val getTrackerProviders: GetTrackerProvidersUseCase,
    private val getCrossMatchedMediaMetadata: GetCrossMatchedMediaMetadataUseCase,
    private val trackerListRepository: TrackerListRepository,
) : GetTrackerListsForMediaUseCase {
    override fun invoke(media: MediaMetadata): Flow<TrackerLists> {
        return getTrackerListsFlow(
            getTrackerProviders = getTrackerProviders,
            repository = trackerListRepository,
        ).flatMapLatest { state ->
            if (state.lists.isEmpty()) {
                return@flatMapLatest flowOf(state)
            }

            channelFlow {
                launch { loadMemberships(media, state.lists) }

                trackerListRepository.getMembership(media.id).collect { membership ->
                    send(state.withMembership(membership))
                }
            }
        }.distinctUntilChanged()
    }

    private suspend fun loadMemberships(
        media: MediaMetadata,
        lists: List<TrackerListAndProvider>,
    ) {
        lists.groupBy { it.list.providerId }
            .entries
            .mapAsync { (providerId, group) ->
                val matched = runCatching {
                    if (media.providerId == providerId) {
                        media
                    } else {
                        getCrossMatchedMediaMetadata(media, providerId)
                    }
                }.getOrNull() ?: return@mapAsync

                group.mapAsync {
                    trackerListRepository.loadMembership(
                        mediaId = media.id,
                        list = it.list,
                        media = matched,
                    )
                }
            }
    }

    private fun TrackerLists.withMembership(
        membership: Map<TrackerListKey, Boolean>,
    ): TrackerLists {
        return copy(
            lists = lists.map {
                val key = TrackerListKey(providerId = it.list.providerId, listId = it.list.id)
                it.copy(containsMedia = membership[key] == true)
            },
        )
    }
}
