package com.flixclusive.data.downloads.di

import com.flixclusive.data.downloads.repository.DownloadRepository
import com.flixclusive.data.downloads.repository.impl.DownloadRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataDownloadModule {
    @Binds
    @Singleton
    abstract fun bindDownloadRepository(impl: DownloadRepositoryImpl): DownloadRepository
}
