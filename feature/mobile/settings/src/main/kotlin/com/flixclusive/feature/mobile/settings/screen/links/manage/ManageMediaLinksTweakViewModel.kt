package com.flixclusive.feature.mobile.settings.screen.links.manage

import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastFlatMap
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.provider.repository.MediaLinksRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.model.provider.ProviderMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
@HiltViewModel
internal class ManageMediaLinksTweakViewModel @Inject constructor(
    private val mediaLinksRepository: MediaLinksRepository,
    private val providerRepository: ProviderRepository,
    private val userSessionDataStore: UserSessionDataStore,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val args = savedStateHandle.navArgs<ManageMediaLinksTweakScreenArgs>()

    private val _typeFilters = MutableStateFlow(LinkType.All)
    val typeFilters = _typeFilters.asStateFlow()

    private val selectedFilters = mutableStateSetOf<String>()

    val providerFilters = combine(
        snapshotFlow { selectedFilters }.distinctUntilChanged(),
        userSessionDataStore.currentUserId.filterNotNull(),
    ) { filters, userId ->
        filters to userId
    }.flatMapLatest { (filters, userId) ->
        mediaLinksRepository
            .observeAllByMedia(
                ownerId = userId,
                mediaId = args.media.id,
            ).distinctUntilChanged()
            .flatMapLatest { cache ->
                val providers = cache.fastMap { it.providerId }
                val flows = providers.fastMap { providerId ->
                    providerRepository
                        .getProviderAsFlow(
                            id = providerId,
                            ownerId = userId
                        ).mapNotNull {
                            val provider = it?.metadata ?: return@mapNotNull null
                            ProviderFilterState(
                                provider = provider,
                                selected = providerId in providers
                            )
                        }
                }

                combine(flows) { it.toList() }
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val links = combine(
        userSessionDataStore.currentUserId.filterNotNull(),
        typeFilters.debounce(300L.milliseconds).distinctUntilChanged(),
        snapshotFlow { selectedFilters }.distinctUntilChanged(),
    ) { userId, typeFilter, filters ->
        Triple(userId, typeFilter, filters)
    }.flatMapLatest { (userId, typeFilter, filters) ->
        mediaLinksRepository
            .observeAllByMedia(
                ownerId = userId,
                mediaId = args.media.id,
            ).mapLatest { cache ->
                var filteredCache = cache
                if (filters.isNotEmpty()) {
                    filteredCache = cache
                        .fastFilter { link ->
                            link.providerId in filters
                        }
                }

                val streams = filteredCache.fastFlatMap { it.streams }
                val subtitles = filteredCache.fastFlatMap { it.subtitles }

                val filteredLinks = when (typeFilter) {
                    LinkType.All -> streams + subtitles
                    LinkType.Streams -> streams
                    LinkType.Subtitles -> subtitles
                }

                Async.Success(
                    filteredLinks.sortedByDescending {
                        it.createdAt.time + (if (it.isDead) 0 else 1)
                    }
                )
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = Async.Loading,
    )

    fun onUpdateTypeFilter(typeFilter: LinkType) {
        _typeFilters.value = typeFilter
    }

    fun onUpdateProviderFilter(providerFilter: ProviderFilterState) {
        if (providerFilter.selected) {
            selectedFilters.add(providerFilter.provider.id)
        } else {
            selectedFilters.remove(providerFilter.provider.id)
        }
    }
}

internal data class ManageMediaLinksTweakUiState(
    val typeFilters: LinkType,
    val providerFilters: Set<ProviderFilterState>,
)

internal data class ProviderFilterState(
    val provider: ProviderMetadata,
    val selected: Boolean
)

internal enum class LinkType {
    All,
    Streams,
    Subtitles
}
