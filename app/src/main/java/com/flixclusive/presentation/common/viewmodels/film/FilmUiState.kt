package com.flixclusive.presentation.common.viewmodels.film

import com.flixclusive.common.UiText
import com.flixclusive.presentation.tv.utils.ModifierTvUtils

data class FilmUiState(
    val isLoading: Boolean = true,
    val errorMessage: UiText? = null,
    val isFilmInWatchlist: Boolean = false,
    val lastFocusedItem: ModifierTvUtils.FocusPosition? = null
)