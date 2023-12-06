package com.flixclusive.presentation.common.viewmodels.home

import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.presentation.tv.utils.ModifierTvUtils.FocusPosition

data class HomeUiState(
    val headerItem: Film? = null,
    val isLoading: Boolean = true,
    val hasErrors: Boolean = false,
    val lastFocusedItem: FocusPosition = FocusPosition(0, 0),
)