package com.flixclusive.feature.mobile.recentlyWatched

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.data.watch_history.WatchHistoryRepository
import com.flixclusive.domain.user.UserSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
internal class RecentlyWatchedScreenViewModel @Inject constructor(
    watchHistoryRepository: WatchHistoryRepository,
    appSettingsManager: AppSettingsManager,
    userSessionManager: UserSessionManager
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
            watchHistoryRepository.getAllItemsInFlow(ownerId = user.id)
                .mapLatest { list ->
                    list.map { it.film }
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}