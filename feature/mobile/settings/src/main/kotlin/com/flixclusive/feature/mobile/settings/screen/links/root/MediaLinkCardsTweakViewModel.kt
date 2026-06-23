package com.flixclusive.feature.mobile.settings.screen.links.root

import androidx.compose.runtime.Stable
import androidx.compose.ui.util.fastSumBy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.database.entity.media.DBMedia.Companion.toMediaMetadata
import com.flixclusive.core.database.entity.media.DBMediaExternalId.Companion.toExternalIdMap
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.DataStoreManager.Companion.getUserPrefsAsFlow
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.model.user.UiPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.data.provider.repository.MediaLinksRepository
import com.flixclusive.model.media.MediaMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
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
internal class MediaLinkCardsTweakViewModel @Inject constructor(
    private val mediaLinksRepository: MediaLinksRepository,
    private val userSessionDataStore: UserSessionDataStore,
    dataStoreManager: DataStoreManager
) : ViewModel() {
    private var initJob: Job? = null

    private val _cacheList = MutableStateFlow<Async<List<MediaWithCachedLinksSize>>>(Async.Loading)
    val cacheList = _cacheList.asStateFlow()

    private val _mediaSort = MutableStateFlow<MediaSortType>(MediaSortType.LinksCount(asc = false))
    val mediaSort = _mediaSort.asStateFlow()

    val showMediaTitles = dataStoreManager
        .getUserPrefsAsFlow<UiPreferences>(UserPreferences.UI_PREFS_KEY)
        .map { it.shouldShowTitleOnCards }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = false,
        )

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
                            val groupedLinksByMedia = links.groupBy { it.media.id }

                            groupedLinksByMedia.mapNotNull { (_, caches) ->
                                if (caches.isEmpty()) return@mapNotNull null

                                val firstCache = caches.firstOrNull() ?: return@mapNotNull null
                                val size = caches.fastSumBy { it.size }

                                MediaWithCachedLinksSize(
                                    size = size,
                                    media = firstCache.media
                                        .toMediaMetadata(
                                            externalIds = firstCache.externalIds
                                                .toExternalIdMap()
                                        ),
                                )
                            }
                        }
                }.onStart { _cacheList.value = Async.Loading }
                .onEach { _cacheList.value = Async.Success(it) }
                .catch { _cacheList.value = Async.Failure(it) }
                .collect()
        }
    }

    fun onMediaSortChange(type: MediaSortType) {
        _mediaSort.value = type
    }
}

@Stable
internal data class MediaWithCachedLinksSize(
    val media: MediaMetadata,
    val size: Int
)

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
