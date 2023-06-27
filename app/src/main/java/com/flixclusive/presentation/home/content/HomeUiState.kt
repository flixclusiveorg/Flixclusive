package com.flixclusive.presentation.home.content

import com.flixclusive.domain.model.tmdb.Film

data class HomeUiState(
    val headerItem: Film? = null,
    val isLoading: Boolean = true,
    val hasErrors: Boolean = false
)
