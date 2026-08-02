package com.flixclusive.data.downloads.di

import com.flixclusive.data.downloads.repository.MediaDownloadRepository
import com.flixclusive.data.downloads.repository.impl.MediaDownloadRepositoryImpl
import com.flixclusive.data.downloads.transfer.MediaTransferEngine
import com.flixclusive.data.downloads.transfer.impl.MediaTransferEngineImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class MediaTransferModule {
    @Binds
    abstract fun bindMediaTransferEngine(impl: MediaTransferEngineImpl): MediaTransferEngine

    @Binds
    @Singleton
    abstract fun bindMediaDownloadRepository(impl: MediaDownloadRepositoryImpl): MediaDownloadRepository
}
