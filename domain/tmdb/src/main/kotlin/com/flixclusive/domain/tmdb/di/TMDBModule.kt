package com.flixclusive.domain.tmdb.di

import com.flixclusive.domain.tmdb.usecase.PaginateTMDBCatalogUseCase
import com.flixclusive.domain.tmdb.usecase.impl.PaginateTMDBCatalogUseCaseImpl
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
    internal abstract fun bindPaginateTMDBCatalogUseCase(
        paginateTMDBCatalogUseCaseImpl: PaginateTMDBCatalogUseCaseImpl
    ): PaginateTMDBCatalogUseCase
}
