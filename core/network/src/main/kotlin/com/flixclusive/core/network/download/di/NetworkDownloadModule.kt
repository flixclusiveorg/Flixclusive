package com.flixclusive.core.network.download.di

import com.flixclusive.core.network.download.CoroutineDownloader
import com.flixclusive.core.network.download.impl.CoroutineDownloaderImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NetworkDownloadModule {
    @Binds
    abstract fun bindCoroutineDownloader(impl: CoroutineDownloaderImpl): CoroutineDownloader
}
