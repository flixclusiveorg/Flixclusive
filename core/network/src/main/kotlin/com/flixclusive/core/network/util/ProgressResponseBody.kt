package com.flixclusive.core.network.util

import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Source
import okio.buffer
import java.io.IOException

internal class ProgressResponseBody(
    private val responseBody: ResponseBody,
    private val progressListener: ProgressListener,
) : ResponseBody() {
    private var bufferedSource: BufferedSource? = null

    override fun contentType(): MediaType? = responseBody.contentType()

    override fun contentLength(): Long = responseBody.contentLength()

    override fun source(): BufferedSource {
        if (bufferedSource == null) {
            bufferedSource = source(responseBody.source()).buffer()
        }

        return bufferedSource!!
    }

    private fun source(source: Source): Source {
        return object : ForwardingSource(source) {
            var totalBytesRead = 0L

            @Throws(IOException::class)
            override fun read(
                sink: Buffer,
                byteCount: Long,
            ): Long {
                val bytesRead = super.read(sink, byteCount)
                totalBytesRead += if (bytesRead != -1L) bytesRead else 0

                progressListener.update(
                    totalBytesRead,
                    responseBody.contentLength(),
                    bytesRead == -1L,
                )
                return bytesRead
            }
        }
    }
}
