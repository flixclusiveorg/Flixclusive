package com.flixclusive.domain.catalog.di

import com.flixclusive.domain.catalog.usecase.GetDiscoverCardsUseCase
import com.flixclusive.domain.catalog.usecase.GetHomeCatalogsUseCase
import com.flixclusive.domain.catalog.usecase.GetHomeHeaderUseCase
import com.flixclusive.domain.catalog.usecase.PaginateItemsUseCase
import com.flixclusive.domain.catalog.usecase.impl.GetDiscoverCardsUseCaseImpl
import com.flixclusive.domain.catalog.usecase.impl.GetHomeCatalogsUseCaseImpl
import com.flixclusive.domain.catalog.usecase.impl.GetHomeHeaderUseCaseImpl
import com.flixclusive.domain.catalog.usecase.impl.PaginateItemsUseCaseImpl
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
    abstract fun bindGetDiscoverCardsUseCase(impl: GetDiscoverCardsUseCaseImpl): GetDiscoverCardsUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindGetHomeCatalogsUseCase(impl: GetHomeCatalogsUseCaseImpl): GetHomeCatalogsUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindGetHomeHeaderUseCase(impl: GetHomeHeaderUseCaseImpl): GetHomeHeaderUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindPaginateItemsUseCase(impl: PaginateItemsUseCaseImpl): PaginateItemsUseCase
}
