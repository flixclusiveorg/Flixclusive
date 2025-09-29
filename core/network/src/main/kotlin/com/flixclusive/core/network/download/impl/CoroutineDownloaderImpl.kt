package com.flixclusive.core.network.download.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.network.download.CoroutineDownloader
import com.flixclusive.core.network.download.DownloadProgress
import com.flixclusive.core.network.util.ProgressInterceptor.Companion.addProgressListener
import com.flixclusive.core.network.util.ProgressListener
import com.flixclusive.core.util.android.saveTo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import javax.inject.Inject

internal class CoroutineDownloaderImpl
    @Inject
    constructor(
        client: OkHttpClient,
        private val appDispatchers: AppDispatchers,
    ) : CoroutineDownloader {
        // Remove caching to avoid issues with downloading the same file multiple times
        // and ensure we always get the latest version
        private val client = client
            .newBuilder()
            .cache(null)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        override fun download(
            url: String,
            destinationFile: File,
        ): Flow<DownloadProgress> =
            channelFlow {
                val request = Request.Builder().url(url).build()

                val response = client
                    .addProgressListener(
                        object : ProgressListener {
                            override fun update(
                                bytesRead: Long,
                                contentLength: Long,
                                done: Boolean,
                            ) {
                                val progress = if (contentLength != -1L) {
                                    ((bytesRead * 100) / contentLength).toInt()
                                } else {
                                    -1
                                }

                                try {
                                    trySend(
                                        DownloadProgress(
                                            bytesDownloaded = bytesRead,
                                            totalBytes = contentLength,
                                            progress = progress,
                                            isComplete = done,
                                        ),
                                    )
                                } catch (e: Exception) {
                                    // Flow collector might be closed, ignore
                                }
                            }
                        },
                    ).newCall(request)
                    .execute()

                if (!response.isSuccessful) {
                    response.close()
                    throw IOException("Failed to download file: ${response.code}")
                }

                response.body.source().saveTo(destinationFile)
                response.close()
            }.flowOn(appDispatchers.io)
    }
