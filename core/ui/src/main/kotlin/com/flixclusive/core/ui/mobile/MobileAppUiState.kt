package com.flixclusive.core.ui.mobile

import com.flixclusive.core.model.provider.SourceDataState

data class MobileAppUiState(
    val isInPlayer: Boolean = false,
    val isShowingUpdateDialog: Boolean = false,
    val isShowingBottomNavigationBar: Boolean = true,
    val isShowingBottomSheetCard: Boolean = false,
    val isLongClickedFilmInWatchlist: Boolean = false,
    val isLongClickedFilmInWatchHistory: Boolean = false,
    val isSeeingMoreDetailsOfLongClickedFilm: Boolean = false,
    val sourceDataState: SourceDataState = SourceDataState.Idle,
    val isInPipMode: Boolean = false,
)
