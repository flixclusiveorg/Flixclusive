package com.flixclusive.data.library.custom.di

import com.flixclusive.data.library.custom.DefaultLibraryListRepository
import com.flixclusive.data.library.custom.LibraryListRepository
import com.flixclusive.data.library.custom.local.LibraryListDataSource
import com.flixclusive.data.library.custom.local.LocalLibraryListDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class LibraryListDataModule {
    @Singleton
    @Binds
    abstract fun bindsLibraryListRepository(
        libraryListRepository: DefaultLibraryListRepository,
    ): LibraryListRepository

    @Singleton
    @Binds
    abstract fun bindsLocalLibraryListDataSource(
        localDataSource: LocalLibraryListDataSource,
    ): LibraryListDataSource
}
