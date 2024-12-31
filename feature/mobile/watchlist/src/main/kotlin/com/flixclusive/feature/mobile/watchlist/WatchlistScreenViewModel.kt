package com.flixclusive.feature.mobile.watchlist

import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.data.watchlist.WatchlistRepository
import com.flixclusive.domain.user.UserSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
internal class WatchlistScreenViewModel @Inject constructor(
    appSettingsManager: AppSettingsManager,
    private val watchlistRepository: WatchlistRepository,
    val userSessionManager: UserSessionManager
) : ViewModel() {
    val appSettings = appSettingsManager.appSettings
        .data
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = appSettingsManager.cachedAppSettings
        )

    val items = userSessionManager.currentUser
        .filterNotNull()
        .flatMapLatest { user ->
            watchlistRepository.getAllItemsInFlow(user.id)
        }
        .mapLatest { list ->
            list.fastMap { it.film }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}