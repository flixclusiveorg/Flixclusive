package com.flixclusive.data.tmdb.di

import com.flixclusive.data.tmdb.DefaultTMDBRepository
import com.flixclusive.data.tmdb.TMDBRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TmdbDataModule {
    @Singleton
    @Binds
    internal abstract fun bindsTMDBRepository(
        tmdbRepository: DefaultTMDBRepository,
    ): TMDBRepository

}