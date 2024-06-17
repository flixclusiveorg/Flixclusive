package com.flixclusive.mobile

import com.flixclusive.model.provider.SourceDataState

internal data class MobileAppUiState(
    val isOnPlayerScreen: Boolean = false,
    val isShowingUpdateDialog: Boolean = false,
    val isShowingBottomSheetCard: Boolean = false,
    val isLongClickedFilmInWatchlist: Boolean = false,
    val isLongClickedFilmInWatchHistory: Boolean = false,
    val sourceDataState: SourceDataState = SourceDataState.Idle,
    val isOnPipMode: Boolean = false,
)
