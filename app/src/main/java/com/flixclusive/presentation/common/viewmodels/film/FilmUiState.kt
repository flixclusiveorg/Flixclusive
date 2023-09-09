package com.flixclusive.presentation.common.viewmodels.film

import com.flixclusive.presentation.common.viewmodels.home.FocusPosition

data class FilmUiState(
    val isLoading: Boolean = true,
    val hasErrors: Boolean = false,
    val isFilmInWatchlist: Boolean = false,
    val lastFocusedItem: FocusPosition? = null
)