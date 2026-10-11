package com.flixclusive.core.presentation.player

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import com.flixclusive.core.util.log.errorLog
import java.io.IOException
import java.io.InterruptedIOException

@OptIn(UnstableApi::class)
internal class LenientDataSource(
    private val upstream: DataSource,
) : DataSource {
    private var failed = false

    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        failed = false

        return try {
            upstream.open(dataSpec)
        } catch (e: InterruptedIOException) {
            throw e
        } catch (e: IOException) {
            errorLog(e)
            failed = true
            0L
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (failed) return C.RESULT_END_OF_INPUT

        return try {
            upstream.read(buffer, offset, length)
        } catch (e: InterruptedIOException) {
            throw e
        } catch (e: IOException) {
            errorLog(e)
            failed = true
            C.RESULT_END_OF_INPUT
        }
    }

    override fun getUri(): Uri? = if (failed) null else upstream.uri

    override fun getResponseHeaders(): Map<String, List<String>> =
        if (failed) emptyMap() else upstream.responseHeaders

    override fun close() {
        failed = false
        upstream.close()
    }

    class Factory(
        private val upstream: DataSource.Factory,
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource = LenientDataSource(upstream.createDataSource())
    }
}
