package com.flixclusive.domain.usecase

import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.presentation.common.UiText
import kotlinx.coroutines.flow.Flow

const val TRENDING_FLAG = "trending"
const val TOP_MOVIE_FLAG = "top_movie"
const val TOP_TV_FLAG = "top_tv"
const val POPULAR_MOVIE_FLAG = "popular_movie"
const val POPULAR_TV_FLAG = "popular_tv"

data class HomeItemConfig(
    val flag: String? = null,
    val label: UiText,
    val data: List<Film>
)

interface HomeItemsProviderUseCase {
    fun getMainRowItems(): Flow<HomeItemConfig?>
    fun getWatchProvidersRowItems(count: Int = 2): Flow<HomeItemConfig?>
    fun getGenreRowItems(count: Int = 2): Flow<HomeItemConfig?>
    fun getBasedOnRowItems(count: Int = 2): Flow<HomeItemConfig?>
    suspend fun getHeaderItem(): Film?
}