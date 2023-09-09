package com.flixclusive.presentation.mobile.main

import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.domain.model.VideoDataDialogState

data class MainUiState(
    val longClickedFilm: Film? = null,
    val preferredServer: String? = null,
    val isShowingUpdateDialog: Boolean = false,
    val isShowingBottomNavigationBar: Boolean = true,
    val isShowingBottomSheetCard: Boolean = false,
    val isLongClickedFilmInWatchlist: Boolean = false,
    val isLongClickedFilmInWatchHistory: Boolean = false,
    val isSeeingMoreDetailsOfLongClickedFilm: Boolean = false,
    val videoDataDialogState: VideoDataDialogState = VideoDataDialogState.Idle,
    val episodeToPlay: TMDBEpisode? = null
)
