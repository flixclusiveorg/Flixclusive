package com.flixclusive.domain.catalog.di

import com.flixclusive.domain.catalog.usecase.GetCatalogItemsUseCase
import com.flixclusive.domain.catalog.usecase.GetHomeCatalogsUseCase
import com.flixclusive.domain.catalog.usecase.impl.GetCatalogItemsUseCaseImpl
import com.flixclusive.domain.catalog.usecase.impl.GetHomeCatalogsUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
internal abstract class DomainCatalogModule {
    @Binds
    @ViewModelScoped
    abstract fun bindGetHomeCatalogsUseCase(impl: GetHomeCatalogsUseCaseImpl): GetHomeCatalogsUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindPaginateItemsUseCase(impl: GetCatalogItemsUseCaseImpl): GetCatalogItemsUseCase
}
