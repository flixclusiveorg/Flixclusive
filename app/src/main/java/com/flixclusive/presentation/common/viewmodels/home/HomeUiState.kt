package com.flixclusive.presentation.common.viewmodels.home

import com.flixclusive.domain.model.tmdb.Film

data class FocusPosition(
    val row: Int,
    val column: Int
)

data class HomeUiState(
    val headerItem: Film? = null,
    val isLoading: Boolean = true,
    val hasErrors: Boolean = false,
    val lastFocusedItem: FocusPosition = FocusPosition(0, 0),
)