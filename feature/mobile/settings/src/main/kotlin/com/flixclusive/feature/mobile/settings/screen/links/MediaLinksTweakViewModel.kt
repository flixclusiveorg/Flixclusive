package com.flixclusive.feature.mobile.settings.screen.links

import androidx.compose.runtime.Stable
import androidx.compose.ui.util.fastSumBy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.database.entity.media.DBMedia
import com.flixclusive.core.database.entity.provider.CachedMediaLinks
import com.flixclusive.core.database.entity.provider.CachedMediaLinksWithData
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.model.user.UiPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.data.provider.repository.MediaLinksRepository
import com.flixclusive.domain.provider.usecase.get.GetProviderMetadataUseCase
import com.flixclusive.domain.provider.usecase.links.TestLinksProgress
import com.flixclusive.domain.provider.usecase.links.TestMediaLinksUseCase
import com.flixclusive.model.provider.ProviderMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class MediaLinksTweakViewModel @Inject constructor(
    private val mediaLinksRepository: MediaLinksRepository,
    private val testMediaLinksUseCase: TestMediaLinksUseCase,
    private val getProviderMetadata: GetProviderMetadataUseCase,
    private val appDispatchers: AppDispatchers,
    private val userSessionDataStore: UserSessionDataStore,
    dataStoreManager: DataStoreManager
) : ViewModel() {
    private var initJob: Job? = null

    private val providerLookupMap = mutableMapOf<String, ProviderMetadata>()

    private val _media = MutableStateFlow<Async<List<MediaWithCachedLinks>>>(Async.Loading)
    val media = _media.asStateFlow()

    private val _selectedCache = MutableStateFlow<CachedMediaLinks?>(null)
    val selectedCache: StateFlow<CachedMediaLinks?> = _selectedCache

    private val _mediaSort = MutableStateFlow<MediaSortType>(MediaSortType.LinksCount(asc = false))
    val mediaSort = _mediaSort.asStateFlow()

    private val _linksSortType = MutableStateFlow<LinksSortType>(LinksSortType.GeneratedAt(asc = true))
    val linksSortType = _linksSortType.asStateFlow()

    private val selectedEntryId = MutableStateFlow<String?>(null)

    val selectedEntry: StateFlow<CachedMediaLinksWithData?> = selectedEntryId
        .filterNotNull()
        .flatMapLatest { id ->
            mediaLinksRepository.observeById(id)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    val showMediaTitles = dataStoreManager
        .getUserPrefsAsFlow(UserPreferences.UI_PREFS_KEY, UiPreferences::class)
        .map { it.shouldShowTitleOnCards }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    private val _testState = MutableStateFlow<Async<TestLinksProgress>?>(null)
    val testState: StateFlow<Async<TestLinksProgress>?> = _testState

    init {
        initialize()
    }

    fun initialize() {
        if (initJob?.isActive == true) return

        initJob = viewModelScope.launch {
            userSessionDataStore.currentUserId
                .filterNotNull()
                .flatMapLatest { id ->
                    mediaLinksRepository
                        .observeAll(id)
                        .mapLatest { links ->
                            val groupedLinksByMedia = links.groupBy { it.cache.mediaId }

                            groupedLinksByMedia.mapNotNull { (_, caches) ->
                                if (caches.isEmpty()) return@mapNotNull null

                                val media = caches.firstOrNull()?.media ?: return@mapNotNull null
                                val cachesWithProvider = caches.mapNotNull { cache ->
                                    val providerId = cache.providerId

                                    val provider = providerLookupMap[providerId]
                                        ?: getProviderMetadata(id = providerId)
                                        ?: return@mapNotNull null

                                    providerLookupMap[providerId] = provider

                                    ProviderWithCachedLinks(
                                        cacheWithData = cache,
                                        provider = provider,
                                    )
                                }

                                MediaWithCachedLinks(
                                    cache = cachesWithProvider,
                                    media = media,
                                )
                            }
                        }
                }.onStart { _media.value = Async.Loading }
                .onEach { _media.value = Async.Success(it) }
                .catch { _media.value = Async.Failure(it) }
                .collect()
        }
    }

    fun selectCache(cache: CachedMediaLinks) {
        _selectedCache.value = cache
    }

    fun onMediaSortChange(type: MediaSortType) {
        _mediaSort.value = type
    }

    fun deleteEntry(entry: CachedMediaLinks) {
        viewModelScope.launch(appDispatchers.io) {
            mediaLinksRepository.deleteById(entry.id)
        }
    }

    fun onTestLinks() {
        val id = selectedEntryId.value ?: return
        viewModelScope.launch {
            _testState.value = Async.Loading

            val entry = mediaLinksRepository.getById(id)

            if (entry == null) {
                _testState.value = Async.Failure(IllegalArgumentException("Selected entry not found"))
                return@launch
            }

            testMediaLinksUseCase(entry.streams + entry.subtitles)
                .catch { e -> _testState.value = Async.Failure(e) }
                .collect { progress ->
                    _testState.value = Async.Success(progress)
                }
        }
    }

    fun dismissTestState() {
        _testState.value = null
    }
}

@Stable
internal data class MediaWithCachedLinks(
    val cache: List<ProviderWithCachedLinks>,
    val media: DBMedia,
) {
    val size get() = cache.fastSumBy { it.size }
}

@Stable
internal data class ProviderWithCachedLinks(
    val cacheWithData: CachedMediaLinksWithData,
    val provider: ProviderMetadata,
) {
    val size: Int get() = cacheWithData.streams.size + cacheWithData.subtitles.size
    val mediaId: String get() = cacheWithData.cache.mediaId
    val seasonNumber: Int? get() = cacheWithData.cache.seasonNumber
    val episodeNumber: Int? get() = cacheWithData.cache.episodeNumber
}

@Stable
internal sealed class MediaSortType(
    val asc: Boolean
) {
    class LinksCount(
        asc: Boolean
    ) : MediaSortType(asc)

    class Title(
        asc: Boolean
    ) : MediaSortType(asc)

    fun toggle(): MediaSortType {
        return when (this) {
            is LinksCount -> LinksCount(asc = !asc)
            is Title -> Title(asc = !asc)
        }
    }

    fun changeType(): MediaSortType {
        return when (this::class) {
            LinksCount::class -> Title(asc = asc)
            Title::class -> LinksCount(asc = asc)
            else -> throw IllegalArgumentException("Unknown MediaSortType: $this")
        }
    }
}

@Stable
internal sealed class LinksSortType(
    val asc: Boolean
) {
    class Url(
        asc: Boolean
    ) : LinksSortType(asc)

    class Dead(
        asc: Boolean
    ) : LinksSortType(asc)

    class GeneratedAt(
        asc: Boolean
    ) : LinksSortType(asc)
}
