package com.flixclusive.feature.mobile.watchlist

import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.util.asStateFlow
import com.flixclusive.data.library.watchlist.WatchlistRepository
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
internal class WatchlistScreenViewModel @Inject constructor(
    dataStoreManager: DataStoreManager,
    private val watchlistRepository: WatchlistRepository,
    userSessionManager: UserSessionManager
) : ViewModel() {
    val uiPreferences = dataStoreManager
        .getUserPrefs<UiPreferences>(UserPreferences.UI_PREFS_KEY)
        .asStateFlow(viewModelScope)

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
