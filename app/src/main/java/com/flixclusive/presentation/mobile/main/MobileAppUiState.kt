package com.flixclusive.presentation.mobile.main

import com.flixclusive.domain.model.VideoDataDialogState
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.TMDBEpisode

data class MobileAppUiState(
    val longClickedFilm: Film? = null,
    val isShowingUpdateDialog: Boolean = false,
    val isShowingBottomNavigationBar: Boolean = true,
    val isShowingBottomSheetCard: Boolean = false,
    val isLongClickedFilmInWatchlist: Boolean = false,
    val isLongClickedFilmInWatchHistory: Boolean = false,
    val isSeeingMoreDetailsOfLongClickedFilm: Boolean = false,
    val videoDataDialogState: VideoDataDialogState = VideoDataDialogState.Idle,
    val episodeToPlay: TMDBEpisode? = null
)
