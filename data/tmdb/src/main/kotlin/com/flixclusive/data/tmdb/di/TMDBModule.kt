package com.flixclusive.data.tmdb.di

import com.flixclusive.data.tmdb.repository.TMDBAssetsRepository
import com.flixclusive.data.tmdb.repository.TMDBDiscoverCatalogRepository
import com.flixclusive.data.tmdb.repository.TMDBFilmSearchItemsRepository
import com.flixclusive.data.tmdb.repository.TMDBHomeCatalogRepository
import com.flixclusive.data.tmdb.repository.TMDBMetadataRepository
import com.flixclusive.data.tmdb.repository.TMDBMovieCollectionRepository
import com.flixclusive.data.tmdb.repository.TMDBWatchProvidersRepository
import com.flixclusive.data.tmdb.repository.impl.TMDBAssetsRepositoryImpl
import com.flixclusive.data.tmdb.repository.impl.TMDBDiscoverCatalogRepositoryImpl
import com.flixclusive.data.tmdb.repository.impl.TMDBFilmSearchItemsRepositoryImpl
import com.flixclusive.data.tmdb.repository.impl.TMDBHomeCatalogRepositoryImpl
import com.flixclusive.data.tmdb.repository.impl.TMDBMetadataRepositoryImpl
import com.flixclusive.data.tmdb.repository.impl.TMDBMovieCollectionRepositoryImpl
import com.flixclusive.data.tmdb.repository.impl.TMDBWatchProvidersRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class TMDBModule {
    @Binds
    @Singleton
    internal abstract fun bindTMDBDiscoverCatalogRepository(
        tmdbDiscoverCatalogRepositoryImpl: TMDBDiscoverCatalogRepositoryImpl,
    ): TMDBDiscoverCatalogRepository

    @Binds
    @Singleton
    internal abstract fun bindTMDBHomeCatalogRepository(
        tmdbHomeCatalogRepositoryImpl: TMDBHomeCatalogRepositoryImpl,
    ): TMDBHomeCatalogRepository

    @Binds
    @Singleton
    internal abstract fun bindTMDBMetadataRepository(
        tmdbMetadataRepositoryImpl: TMDBMetadataRepositoryImpl,
    ): TMDBMetadataRepository

    @Binds
    @Singleton
    internal abstract fun bindTMDBMovieCollectionRepository(
        tmdbMovieCollectionRepositoryImpl: TMDBMovieCollectionRepositoryImpl,
    ): TMDBMovieCollectionRepository

    @Binds
    @Singleton
    internal abstract fun bindTMDBWatchProvidersRepository(
        tmdbWatchProvidersRepositoryImpl: TMDBWatchProvidersRepositoryImpl,
    ): TMDBWatchProvidersRepository

    @Binds
    @Singleton
    internal abstract fun bindTMDBAssetsRepository(
        tmdbAssetsRepositoryImpl: TMDBAssetsRepositoryImpl,
    ): TMDBAssetsRepository

    @Binds
    @Singleton
    internal abstract fun bindTMDBFilmSearchItemsRepository(
        tmdbFilmSearchItemsRepositoryImpl: TMDBFilmSearchItemsRepositoryImpl,
    ): TMDBFilmSearchItemsRepository
}
