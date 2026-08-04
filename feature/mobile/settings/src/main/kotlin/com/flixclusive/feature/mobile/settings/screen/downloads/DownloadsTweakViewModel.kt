package com.flixclusive.feature.mobile.settings.screen.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.data.downloads.repository.MediaDownloadRepository
import com.flixclusive.domain.downloads.controller.MediaDownloadController
import com.flixclusive.domain.downloads.usecase.GetCompletedDownloadFileUseCase
import com.flixclusive.model.media.common.MediaType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
@HiltViewModel
internal class DownloadsTweakViewModel @Inject constructor(
    private val mediaDownloadRepository: MediaDownloadRepository,
    private val mediaDownloadController: MediaDownloadController,
    private val getCompletedDownloadFile: GetCompletedDownloadFileUseCase,
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    /** Debounced separately from [query] (rather than inline in the [entries] combine) so its
     * [stateIn] can seed an immediate [initialValue] — debouncing [_query] directly inside the
     * combine would delay the very first emission of [entries] by the debounce window too, since
     * `combine` waits on every source's first value. */
    private val debouncedQuery = _query
        .debounce(800.milliseconds)
        .distinctUntilChanged()
        .stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = _query.value)

    private val _stateFilters = MutableStateFlow<Set<DownloadStateFilter>>(emptySet())
    val stateFilters = _stateFilters.asStateFlow()

    private val _typeFilters = MutableStateFlow<Set<MediaType>>(emptySet())
    val typeFilters = _typeFilters.asStateFlow()

    private val _event = MutableSharedFlow<DownloadsTweakEvent>()
    val event = _event.asSharedFlow()

    val entries = combine(
        mediaDownloadRepository.observeAllItems(),
        debouncedQuery,
        _stateFilters,
        _typeFilters,
    ) { items, query, stateFilters, typeFilters ->
        val allowedStates = stateFilters.flatMap { it.states }.toSet()

        items
            .filter { allowedStates.isEmpty() || it.state in allowedStates }
            .filter { typeFilters.isEmpty() || it.mediaType in typeFilters }
            .filter { query.isBlank() || it.mediaTitle.contains(query, ignoreCase = true) }
            .let(::groupIntoEntries)
    }.map { Async.Success(it) as Async<List<DownloadListEntry>> }
        .onStart { emit(Async.Loading) }
        .catch { emit(Async.Failure(it)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Async.Loading,
        )

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun onToggleStateFilter(filter: DownloadStateFilter) {
        _stateFilters.value = _stateFilters.value.toggle(filter)
    }

    fun onToggleTypeFilter(type: MediaType) {
        _typeFilters.value = _typeFilters.value.toggle(type)
    }

    fun onPause(itemId: String) = mediaDownloadController.pause(itemId)

    fun onResume(itemId: String) = mediaDownloadController.resume(itemId)

    fun onStop(itemId: String) = mediaDownloadController.stop(itemId)

    fun onRetry(itemId: String) = mediaDownloadController.retry(itemId)

    fun onDelete(itemId: String) = mediaDownloadController.delete(itemId)

    fun onPauseBatch(mediaId: String, seasonNumber: Int) = mediaDownloadController.pauseBatch(mediaId, seasonNumber)

    fun onStopBatch(mediaId: String, seasonNumber: Int) = mediaDownloadController.stopBatch(mediaId, seasonNumber)

    fun onOpen(item: DownloadItem) {
        viewModelScope.launch {
            // Precondition, not a value we forward — the player resolves the file itself from
            // itemId. Checking here means a missing file shows nothing instead of opening a
            // player that immediately pops.
            getCompletedDownloadFile(item) ?: return@launch
            _event.emit(DownloadsTweakEvent.OpenFile(item.id))
        }
    }

    private fun groupIntoEntries(items: List<DownloadItem>): List<DownloadListEntry> {
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

        return (batches + singles).sortedByDescending { it.latestUpdatedAt }
    }
}

private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value

internal sealed class DownloadsTweakEvent {
    data class OpenFile(
        val itemId: String
    ) : DownloadsTweakEvent()
}
