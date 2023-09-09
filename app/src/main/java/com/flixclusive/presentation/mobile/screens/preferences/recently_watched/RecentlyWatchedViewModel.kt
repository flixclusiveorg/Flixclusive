package com.flixclusive.presentation.mobile.screens.preferences.recently_watched

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.domain.repository.WatchHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class RecentlyWatchedViewModel @Inject constructor(
    watchHistoryRepository: WatchHistoryRepository
) : ViewModel() {
    val items = watchHistoryRepository
        .getAllItemsInFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}