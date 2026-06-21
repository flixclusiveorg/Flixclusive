package com.flixclusive.feature.mobile.settings.screen.links.manage

import android.content.Context
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.database.entity.provider.CachedMediaLink
import com.flixclusive.core.database.entity.watched.EpisodeProgressWithMetadata
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.util.log.warnLog
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.data.provider.repository.MediaLinksRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.usecase.get.GetMediaMetadataUseCase
import com.flixclusive.domain.provider.usecase.get.GetNextEpisodeUseCase
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.Show
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.model.media.common.tv.Season
import com.flixclusive.model.provider.ProviderMetadata
import com.ramcosta.composedestinations.generated.settings.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
@HiltViewModel
internal class ManageMediaLinksTweakViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val mediaLinksRepository: MediaLinksRepository,
    private val providerRepository: ProviderRepository,
    private val getMediaMetadata: GetMediaMetadataUseCase,
    private val getNextEpisode: GetNextEpisodeUseCase,
    private val watchProgressRepository: WatchProgressRepository,
    private val userSessionDataStore: UserSessionDataStore,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val args = savedStateHandle.navArgs<ManageMediaLinksTweakScreenArgs>()

    private val _typeFilter = MutableStateFlow(LinkType.All)
    val typeFilter = _typeFilter.asStateFlow()

    private val selectedProviders = mutableStateSetOf<String>()

    private val _selectedLinks = MutableStateFlow(setOf<CachedMediaLink>())
    val selectedLinks = _selectedLinks.asStateFlow()

    private val _event = MutableSharedFlow<ManageMediaLinksTweakEvent>()
    val event = _event.asSharedFlow()

    val providerFilters = combine(
        userSessionDataStore.currentUserId.filterNotNull(),
        snapshotFlow { selectedProviders.toList() }.distinctUntilChanged(),
    ) { userId, _ ->
        userId
    }.flatMapLatest { userId ->
        mediaLinksRepository
            .observeLinks(
                ownerId = userId,
                mediaId = args.media.id,
                episodeNumber = args.episode?.number,
                seasonNumber = args.episode?.season
            ).distinctUntilChanged()
            .flatMapLatest { cache ->
                val providers = cache.map { it.providerId }.distinct()
                val flows = providers.map { providerId ->
                    providerRepository
                        .getProviderAsFlow(
                            id = providerId,
                            ownerId = userId
                        ).mapNotNull {
                            val provider = it?.metadata ?: return@mapNotNull null
                            ProviderFilterState(
                                provider = provider,
                                selected = provider.id in selectedProviders
                            )
                        }
                }

                if (flows.isEmpty()) {
                    MutableStateFlow(emptyList())
                } else {
                    combine(flows) { it.toList() }
                }
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val links = combine(
        userSessionDataStore.currentUserId.filterNotNull(),
        typeFilter.debounce(300L.milliseconds).distinctUntilChanged(),
        snapshotFlow { selectedProviders.toList() }.distinctUntilChanged(),
    ) { userId, type, _ ->
        userId to type
    }.flatMapLatest { (userId, typeFilter) ->
        mediaLinksRepository
            .observeLinks(
                ownerId = userId,
                mediaId = args.media.id,
                episodeNumber = args.episode?.number,
                seasonNumber = args.episode?.season
            ).mapLatest { cache ->
                var filteredCache = cache
                if (selectedProviders.isNotEmpty()) {
                    filteredCache = cache
                        .filter { link ->
                            link.providerId in selectedProviders
                        }
                }

                val streams = filteredCache.flatMap { it.streams }
                val subtitles = filteredCache.flatMap { it.subtitles }

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

    fun onUpdateTypeFilter(type: LinkType) {
        _typeFilter.value = type
    }

    fun onUpdateProviderFilter(providerId: String) {
        if (providerId in selectedProviders) {
            selectedProviders.remove(providerId)
        } else {
            selectedProviders.add(providerId)
        }
    }

    fun onToggleSelect(link: CachedMediaLink) {
        _selectedLinks.value = if (link in _selectedLinks.value) {
            _selectedLinks.value - link
        } else {
            _selectedLinks.value + link
        }
    }

    fun onClearSelection() {
        _selectedLinks.value = emptySet()
    }

    fun onDeleteLinks(linksToDelete: List<CachedMediaLink> = emptyList()) {
        val targetLinks = linksToDelete.ifEmpty { _selectedLinks.value.toList() }
        if (targetLinks.isEmpty()) return

        viewModelScope.launch {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()
            mediaLinksRepository.deleteLinks(targetLinks.map { it.url }, userId)
            onClearSelection()
        }
    }

    fun onResetLinks(linksToReset: List<CachedMediaLink> = emptyList()) {
        val targetLinks = linksToReset.ifEmpty { _selectedLinks.value.toList() }
        if (targetLinks.isEmpty()) return

        viewModelScope.launch {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()
            targetLinks.forEach { link ->
                mediaLinksRepository.setLinkStatus(
                    url = link.url,
                    ownerId = userId,
                    isDead = false
                )
            }
            onClearSelection()
        }
    }

    private suspend fun getEpisodeToWatch(tvShow: Show): Episode? {
        val userId = userSessionDataStore.currentUserId.filterNotNull().first()
        val progress = watchProgressRepository.get(
            id = tvShow.id,
            ownerId = userId,
            type = tvShow.type,
        ) as? EpisodeProgressWithMetadata

        if (progress?.watchData?.isCompleted == true) {
            return getNextEpisode(
                show = tvShow,
                season = progress.watchData.seasonNumber,
                episode = progress.watchData.episodeNumber,
            )
        }

        val seasonNumber = progress?.watchData?.seasonNumber ?: 1
        val episodeNumber = progress?.watchData?.episodeNumber ?: 1

        val seasonIndex = tvShow.seasons.binarySearch {
            it.number.compareTo(seasonNumber)
        }

        var season = tvShow.seasons.getOrNull(seasonIndex)
        if (season is Season.Partial) {
            val provider = providerRepository.getProvider(
                id = tvShow.providerId,
                ownerId = userId
            )

            val api = provider?.plugin?.getMetadataApi(context)
            if (api == null || !provider.isMetadataEnabled) {
                return null
            }

            season = api.getSeason(
                show = tvShow,
                season = season,
            )
        }

        if (season !is Season.Full) {
            warnLog("Season $seasonNumber not found for show ${tvShow.title} (${tvShow.id})")
            return null
        }

        val episode = season.episodes
            .binarySearch {
                it.number.compareTo(episodeNumber)
            }.let { index -> season.episodes.getOrNull(index) }

        return episode
    }

    fun onPlayLink(link: CachedMediaLink) {
        if (_selectedLinks.value.isNotEmpty()) {
            onToggleSelect(link)
            return
        }

        viewModelScope.launch {
            var metadata = args.media
            if (metadata is PartialMedia) {
                metadata = getMediaMetadata(media = metadata)
                    .last()
                    .let {
                        when (it) {
                            is Async.Success -> it.data
                            else -> return@launch
                        }
                    }
            }

            if (metadata is PartialMedia) return@launch

            var episodeToLoad = args.episode
            if (metadata is Show && args.episode == null) {
                episodeToLoad = getEpisodeToWatch(tvShow = metadata)
            }

            _event.emit(ManageMediaLinksTweakEvent.PlayLink(link, metadata, episodeToLoad))
        }
    }
}

internal sealed class ManageMediaLinksTweakEvent {
    data class PlayLink(
        val link: CachedMediaLink,
        val media: MediaMetadata,
        val episode: Episode?
    ) : ManageMediaLinksTweakEvent()
}

internal data class ProviderFilterState(
    val provider: ProviderMetadata,
    val selected: Boolean
)

internal enum class LinkType {
    All,
    Streams,
    Subtitles
}
