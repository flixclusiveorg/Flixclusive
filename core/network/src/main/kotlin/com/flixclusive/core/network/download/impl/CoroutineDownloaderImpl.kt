package com.flixclusive.core.network.download.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.network.download.CoroutineDownloader
import com.flixclusive.core.network.download.DownloadProgress
import com.flixclusive.core.network.util.ProgressInterceptor.Companion.addProgressListener
import com.flixclusive.core.network.util.ProgressListener
import com.flixclusive.core.util.log.errorLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.BufferedSource
import okio.buffer
import okio.sink
import java.io.File
import java.io.IOException
import javax.inject.Inject

internal class CoroutineDownloaderImpl @Inject constructor(
    client: OkHttpClient,
    private val appDispatchers: AppDispatchers,
) : CoroutineDownloader {
    private val client by lazy {
        client
            .newBuilder()
            .cache(null)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
            .addProgressListener()
    }

    override fun download(
        url: String,
        destinationFile: File,
    ): Flow<DownloadProgress> =
        callbackFlow {
            val request = Request
                .Builder()
                .url(url)
                .tag<ProgressListener>(
                    tag = object : ProgressListener {
                        override fun update(
                            bytesRead: Long,
                            contentLength: Long,
                            done: Boolean,
                        ) {
                            val progress = if (contentLength != -1L) {
                                (bytesRead * 100) / contentLength.toFloat()
                            } else {
                                -1f
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
                            } catch (_: CancellationException) {
                                // Flow collector might be closed, ignore
                            } catch (e: Throwable) {
                                // Log the error but don't crash the downloader
                                e.printStackTrace()
                                errorLog(e)
                            }

                            if (done) {
                                close()
                            }
                        }
                    }
                ).build()

            val response = client
                .newCall(request)
                .execute()

            response.use { res ->
                if (!res.isSuccessful) {
                    throw IOException("Failed to download file: ${res.code}")
                }

                val source = res.body.source()
                try {
                    destinationFile.parentFile?.mkdirs()
                    source.saveTo(destinationFile)
                } catch (e: Throwable) {
                    destinationFile.delete()
                    throw e
                }
            }
        }.flowOn(appDispatchers.io)

    private fun BufferedSource.saveTo(file: File) {
        file.outputStream().sink().buffer().use { sink ->
            sink.writeAll(this)
            sink.flush()
        }
    }
}
