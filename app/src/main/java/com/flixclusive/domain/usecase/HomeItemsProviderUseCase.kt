package com.flixclusive.domain.usecase

import com.flixclusive.domain.model.config.HomeCategoryItem
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.TMDBPageResponse
import com.flixclusive.domain.model.tmdb.TMDBSearchItem
import kotlinx.coroutines.flow.Flow

interface HomeItemsProviderUseCase {
    fun getHomeRecommendations(): Flow<HomeCategoryItem?>
    fun getUserRecommendations(
        userId: Int = 1,
        count: Int = 2,
    ): Flow<HomeCategoryItem?>
    suspend fun getHeaderItem(): Film?

    suspend fun getHomeItems(
        query: String,
        page: Int,
        onFailure: () -> Unit,
        onSuccess: (data: TMDBPageResponse<TMDBSearchItem>) -> Unit,
    )
    suspend fun getFocusedItem(film: Film): Film?
}