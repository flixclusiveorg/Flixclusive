package com.flixclusive.data.downloads.di

import com.flixclusive.data.downloads.hls.HlsManifestResolver
import com.flixclusive.data.downloads.hls.HlsTransferEngine
import com.flixclusive.data.downloads.hls.impl.HlsManifestResolverImpl
import com.flixclusive.data.downloads.hls.impl.HlsTransferEngineImpl
import com.flixclusive.data.downloads.probe.LinkProbe
import com.flixclusive.data.downloads.probe.impl.LinkProbeImpl
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
    abstract fun bindHlsManifestResolver(impl: HlsManifestResolverImpl): HlsManifestResolver

    @Binds
    abstract fun bindHlsTransferEngine(impl: HlsTransferEngineImpl): HlsTransferEngine

    @Binds
    abstract fun bindLinkProbe(impl: LinkProbeImpl): LinkProbe

    @Binds
    @Singleton
    abstract fun bindMediaDownloadRepository(impl: MediaDownloadRepositoryImpl): MediaDownloadRepository
}
