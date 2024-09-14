package com.flixclusive.mobile

import com.flixclusive.core.ui.common.provider.MediaLinkResourceState

internal data class MobileAppUiState(
    val isOnPlayerScreen: Boolean = false,
    val isShowingUpdateDialog: Boolean = false,
    val isShowingBottomSheetCard: Boolean = false,
    val isLongClickedFilmInWatchlist: Boolean = false,
    val isLongClickedFilmInWatchHistory: Boolean = false,
    val mediaLinkResourceState: MediaLinkResourceState = MediaLinkResourceState.Idle,
    val isOnPipMode: Boolean = false,
)
