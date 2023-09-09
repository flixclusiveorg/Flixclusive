package com.flixclusive.domain.usecase

import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.FilmType

interface FilmProviderUseCase {
    suspend operator fun invoke(
        id: Int,
        type: FilmType,
        onError: () -> Unit,
        onSuccess: (Film?) -> Unit,
    )
}