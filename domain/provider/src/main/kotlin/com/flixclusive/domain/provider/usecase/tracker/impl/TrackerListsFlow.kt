package com.flixclusive.domain.provider.usecase.tracker.impl

import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.provider.ProviderWithThrowable
import com.flixclusive.data.provider.repository.ProviderResponseWrapper
import com.flixclusive.data.provider.repository.TrackerListRepository
import com.flixclusive.domain.provider.usecase.get.GetTrackerProvidersUseCase
import com.flixclusive.domain.provider.usecase.tracker.TrackerListAndProvider
import com.flixclusive.domain.provider.usecase.tracker.TrackerLists
import com.flixclusive.provider.tracker.TrackerList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal fun getTrackerListsFlow(
    getTrackerProviders: GetTrackerProvidersUseCase,
    repository: TrackerListRepository,
): Flow<TrackerLists> {
    return getTrackerProviders()
        .flatMapLatest { state ->
            when (state) {
                is Async.Loading -> flowOf(TrackerLists(isLoading = true))
                is Async.Failure -> flowOf(TrackerLists())
                is Async.Success -> {
                    val providers = state.data.filter { it.isTrackerEnabled && it.metadata != null }
                    if (providers.isEmpty()) {
                        return@flatMapLatest flowOf(TrackerLists())
                    }

                    channelFlow {
                        providers.forEach { provider ->
                            launch { repository.loadLists(provider.id) }
                        }

                        combine(
                            providers.map { provider ->
                                repository.getLists(provider.id).map { provider to it }
                            },
                        ) { it.toList() }
                            .collect { send(it.toTrackerLists()) }
                    }
                }
            }
        }.distinctUntilChanged()
}

private fun List<Pair<ProviderResponseWrapper, Async<List<TrackerList>>>>.toTrackerLists(): TrackerLists {
    val lists = mutableListOf<TrackerListAndProvider>()
    val errors = mutableListOf<ProviderWithThrowable>()
    var isLoading = false

    forEach { (provider, state) ->
        val metadata = provider.metadata ?: return@forEach

        when (state) {
            is Async.Loading -> isLoading = true

            is Async.Failure -> {
                errors.add(
                    ProviderWithThrowable(
                        provider = metadata,
                        throwable = state.cause
                            ?: IllegalStateException("Failed to load lists of ${metadata.name}"),
                    ),
                )
            }

            is Async.Success -> {
                state.data.forEach { list ->
                    lists.add(TrackerListAndProvider(list = list, provider = metadata))
                }
            }
        }
    }

    return TrackerLists(lists = lists, errors = errors, isLoading = isLoading)
}
