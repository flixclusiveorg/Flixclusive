package com.flixclusive.feature.mobile.recentlyWatched

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.util.asStateFlow
import com.flixclusive.data.watch_history.WatchHistoryRepository
import com.flixclusive.domain.user.UserSessionManager
import com.flixclusive.model.datastore.user.UiPreferences
import com.flixclusive.model.datastore.user.UserPreferences
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
    dataStoreManager: DataStoreManager,
    userSessionManager: UserSessionManager
) : ViewModel() {
    val uiPreferences = dataStoreManager
        .getUserPrefs<UiPreferences>(UserPreferences.UI_PREFS_KEY)
        .asStateFlow(viewModelScope)

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