package com.flixclusive.data.downloads.di

import com.flixclusive.data.downloads.directory.DownloadDirectoryRepository
import com.flixclusive.data.downloads.directory.impl.DownloadDirectoryRepositoryImpl
import com.flixclusive.data.downloads.hls.HlsManifestResolver
import com.flixclusive.data.downloads.hls.HlsTransferEngine
import com.flixclusive.data.downloads.hls.impl.HlsManifestResolverImpl
import com.flixclusive.data.downloads.hls.impl.HlsTransferEngineImpl
import com.flixclusive.data.downloads.probe.LinkProbe
import com.flixclusive.data.downloads.probe.impl.LinkProbeImpl
import com.flixclusive.data.downloads.repository.DownloadRepository
import com.flixclusive.data.downloads.repository.MediaDownloadRepository
import com.flixclusive.data.downloads.repository.impl.DownloadRepositoryImpl
import com.flixclusive.data.downloads.repository.impl.MediaDownloadRepositoryImpl
import com.flixclusive.data.downloads.transfer.MediaTransferEngine
import com.flixclusive.data.downloads.transfer.impl.MediaTransferEngineImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataDownloadModule {
    @Binds
    @Singleton
    abstract fun bindDownloadRepository(impl: DownloadRepositoryImpl): DownloadRepository

    @Binds
    abstract fun bindDownloadDirectoryRepository(impl: DownloadDirectoryRepositoryImpl): DownloadDirectoryRepository

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

    companion object {
        /**
         * One client for every download transfer, rather than each engine deriving its own copy of
         * the same builder — four identical clients meant four connection pools and four thread
         * pools for what is one job.
         *
         * The call timeout is the part that was missing. [LinkProbeImpl] wrapped its probe in
         * `withTimeoutOrNull`, but the body is blocking, non-suspending I/O with no suspension
         * point for the timeout to cancel at, so nothing actually bounded a server that accepts a
         * connection and then dribbles. A call timeout is enforced by OkHttp itself and does.
         */
        @Provides
        @Singleton
        @DownloadHttpClient
        fun provideDownloadHttpClient(client: OkHttpClient): OkHttpClient =
            client
                .newBuilder()
                .cache(null)
                .followRedirects(true)
                .followSslRedirects(true)
                .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build()

        /** Generous enough for a slow chunk, short enough that a dead connection cannot hang a
         * transfer indefinitely. */
        private const val CALL_TIMEOUT_SECONDS = 120L
    }
}
