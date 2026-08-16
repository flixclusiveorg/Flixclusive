package com.flixclusive.feature.mobile.settings.screen.system.logcat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.domain.Async
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class LogcatTweakViewModel @Inject constructor(
    private val logcatDataSource: LogcatDataSource,
    private val appDispatchers: AppDispatchers,
    @ApplicationContext context: Context,
) : ViewModel() {
    private val filterContext = LogFilterContext(
        ownPid = logcatDataSource.ownPid,
        ownPackageName = context.packageName,
    )

    private val _filterQuery = MutableStateFlow(DEFAULT_FILTER_QUERY)
    val filterQuery = _filterQuery.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _levelFilters = MutableStateFlow<ImmutableSet<LogLevel>>(persistentSetOf())
    val levelFilters = _levelFilters.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused = _isPaused.asStateFlow()

    /** Clearing hides everything logged so far rather than running `logcat -c`, which wipes a buffer
     * shared with the rest of the system and can fail without saying so. */
    private val clearedBeforeId = MutableStateFlow(NOTHING_CLEARED)

    /** Bumped by [onReload] to resubscribe after a failure. The catch below terminates the flow, so
     * without a fresh subscription the error state would be permanent. */
    private val retryTrigger = MutableStateFlow(0)

    /** Debounced separately from the combine below so its [stateIn] can seed an immediate value —
     * debouncing inside the combine would delay the very first emission too, since `combine` waits
     * on every source's first value. */
    private val debouncedFilter = _filterQuery
        .debounce(FILTER_DEBOUNCE)
        .distinctUntilChanged()
        .map(LogFilterParser::parse)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = LogFilterParser.parse(_filterQuery.value),
        )

    private val debouncedSearch = _searchQuery
        .debounce(SEARCH_DEBOUNCE)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = _searchQuery.value,
        )

    private val rawEntries: StateFlow<Async<List<LogEntry>>> = retryTrigger
        .flatMapLatest {
            logcatDataSource
                .stream()
                .scan(ArrayDeque<LogEntry>(BUFFER_CAPACITY)) { buffer, batch ->
                    buffer.addAll(batch)
                    while (buffer.size > BUFFER_CAPACITY) buffer.removeFirst()
                    buffer
                }
                // Hands downstream an immutable snapshot so the mutable deque is never read from
                // another dispatcher.
                .map { Async.Success(ArrayList(it)) as Async<List<LogEntry>> }
                .onStart { emit(Async.Loading) }
                .catch { emit(Async.Failure(it)) }
        }.flowOn(appDispatchers.default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = Async.Loading,
        )

    /** Pausing freezes the view without losing anything: this still collects [rawEntries] while
     * paused — dropping the subscription would let the WhileSubscribed timeout kill the logcat
     * process and reset the buffer — it just keeps handing the last unpaused snapshot downstream. */
    private val displayedEntries = rawEntries
        .combine(_isPaused) { entries, paused -> entries to paused }
        .scan(Async.Loading as Async<List<LogEntry>>) { previous, (entries, paused) ->
            if (paused) previous else entries
        }.distinctUntilChanged()

    private val criteria = combine(
        debouncedFilter,
        debouncedSearch,
        _levelFilters,
        clearedBeforeId,
        ::Criteria,
    )

    val state: StateFlow<Async<LogcatUiState>> = combine(displayedEntries, criteria) { entries, criteria ->
        when (entries) {
            is Async.Loading -> Async.Loading
            is Async.Failure -> entries
            is Async.Success -> Async.Success(entries.data.toUiState(criteria))
        }
    }.flowOn(appDispatchers.default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = Async.Loading,
        )

    fun onFilterQueryChange(value: String) {
        _filterQuery.value = value
    }

    fun onSearchQueryChange(value: String) {
        _searchQuery.value = value
    }

    fun onToggleLevelFilter(level: LogLevel) {
        _levelFilters.update { current ->
            if (level in current) {
                (current - level).toPersistentSet()
            } else {
                (current + level).toPersistentSet()
            }
        }
    }

    fun onTogglePause() {
        _isPaused.update { !it }
    }

    fun onClear() {
        val entries = (rawEntries.value as? Async.Success)?.data
        clearedBeforeId.value = entries?.lastOrNull()?.id ?: NOTHING_CLEARED
    }

    fun onReload() {
        retryTrigger.update { it + 1 }
    }

    private fun List<LogEntry>.toUiState(criteria: Criteria): LogcatUiState {
        val filter = criteria.parsed.filter
        val visible = ArrayList<LogEntry>(size)
        val matches = ArrayList<Int>()
        var maxLineLength = 0

        forEach { entry ->
            if (entry.id <= criteria.clearedBeforeId) return@forEach
            if (criteria.levels.isNotEmpty() && entry.level !in criteria.levels) return@forEach
            if (!filter.matches(entry, filterContext)) return@forEach

            if (criteria.search.isNotEmpty() && entry.text.contains(criteria.search, ignoreCase = true)) {
                matches += visible.size
            }

            visible += entry
            if (entry.text.length > maxLineLength) maxLineLength = entry.text.length
        }

        return LogcatUiState(
            entries = visible.toPersistentList(),
            matchIndices = matches.toPersistentList(),
            maxLineLength = maxLineLength,
            searchQuery = criteria.search,
            totalCount = size,
            filterError = criteria.parsed.error,
        )
    }

    private data class Criteria(
        val parsed: ParsedFilter,
        val search: String,
        val levels: ImmutableSet<LogLevel>,
        val clearedBeforeId: Long,
    )

    companion object {
        const val DEFAULT_FILTER_QUERY = "package:mine "

        private const val NOTHING_CLEARED = -1L
        private const val BUFFER_CAPACITY = 5_000
        private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
        private val FILTER_DEBOUNCE = 250.milliseconds
        private val SEARCH_DEBOUNCE = 250.milliseconds
    }
}
