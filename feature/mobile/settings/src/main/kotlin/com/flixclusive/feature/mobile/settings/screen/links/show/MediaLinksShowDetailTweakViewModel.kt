package com.flixclusive.feature.mobile.settings.screen.links.show

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.provider.repository.MediaLinksRepository
import com.flixclusive.feature.mobile.settings.screen.links.manage.ManageMediaLinksTweakScreenArgs
import com.ramcosta.composedestinations.generated.settings.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class MediaLinksShowDetailTweakViewModel @Inject constructor(
    private val mediaLinksRepository: MediaLinksRepository,
    private val userSessionDataStore: UserSessionDataStore,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val args = savedStateHandle.navArgs<ManageMediaLinksTweakScreenArgs>()
    val show = args.media

    private val _selectedSeason = MutableStateFlow<Int?>(null)
    val selectedSeason = _selectedSeason.asStateFlow()

    val availableSeasons = userSessionDataStore.currentUserId
        .filterNotNull()
        .flatMapLatest { userId ->
            mediaLinksRepository.observeCachedSeasons(show.id, userId)
        }.map { it.sortedBy { s -> s.number } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val episodes = combine(
        _selectedSeason.filterNotNull(),
        userSessionDataStore.currentUserId.filterNotNull()
    ) { season, userId ->
        season to userId
    }.flatMapLatest { (season, userId) ->
        mediaLinksRepository.observeCachedEpisodes(show.id, season, userId)
    }.map { list ->
        Async.Success(list.sortedBy { it.number })
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Async.Loading
    )

    init {
        viewModelScope.launch {
            val seasons = availableSeasons.first { it.isNotEmpty() }
            if (_selectedSeason.value == null) {
                _selectedSeason.value = seasons.first().number
            }
        }
    }

    fun onSeasonChange(season: Int) {
        _selectedSeason.value = season
    }

    fun onDeleteSeason(season: Int) {
        viewModelScope.launch {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()
            mediaLinksRepository.deleteLinks(userId, show.id, null, season)
        }
    }

    fun onResetEpisode(season: Int, episode: Int) {
        viewModelScope.launch {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()
            val links = mediaLinksRepository.getLinks(userId, show.id, episode, season)
            links.flatMap { it.streams + it.subtitles }.forEach {
                mediaLinksRepository.setLinkStatus(url = it.url, ownerId = userId, isDead = false)
            }
        }
    }
}
