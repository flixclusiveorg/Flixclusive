package com.flixclusive.data.search.di

import com.flixclusive.data.search.DefaultSearchHistoryRepository
import com.flixclusive.data.search.SearchHistoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SearchHistoryDataModule {
    @Singleton
    @Binds
    internal abstract fun bindsSearchHistoryRepository(
        searchHistoryRepository: DefaultSearchHistoryRepository,
    ): SearchHistoryRepository

}
