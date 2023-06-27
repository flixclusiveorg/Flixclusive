package com.flixclusive.presentation.main

import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.presentation.common.VideoDataDialogState

data class MainUiState(
    val longClickedFilm: Film? = null,
    val isShowingUpdateDialog: Boolean = false,
    val isShowingBottomNavigationBar: Boolean = true,
    val isShowingBottomSheetCard: Boolean = false,
    val isLongClickedFilmInWatchlist: Boolean = false,
    val isLongClickedFilmInWatchHistory: Boolean = false,
    val isSeeingMoreDetailsOfLongClickedFilm: Boolean = false,
    val videoDataDialogState: VideoDataDialogState = VideoDataDialogState.IDLE,
    val episodeToPlay: TMDBEpisode? = null
)
