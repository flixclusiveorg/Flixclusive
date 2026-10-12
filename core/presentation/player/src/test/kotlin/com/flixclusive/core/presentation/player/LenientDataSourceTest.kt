package com.flixclusive.core.presentation.player

import android.util.Log
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import java.io.IOException
import java.io.InterruptedIOException

class LenientDataSourceTest {
    private val upstream = mockk<DataSource>(relaxed = true)
    private val dataSpec = mockk<DataSpec>(relaxed = true)
    private val dataSource = LenientDataSource(upstream)

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `open should return the upstream length when it succeeds`() {
        every { upstream.open(dataSpec) } returns 42L

        expectThat(dataSource.open(dataSpec)).isEqualTo(42L)
    }

    @Test
    fun `open should swallow an upstream IOException and report an empty stream`() {
        every { upstream.open(dataSpec) } throws IOException("HTTP 404")

        expectThat(dataSource.open(dataSpec)).isEqualTo(0L)
        expectThat(dataSource.read(ByteArray(8), 0, 8)).isEqualTo(C.RESULT_END_OF_INPUT)
        expectThat(dataSource.uri).isNull()
        expectThat(dataSource.responseHeaders).isEmpty()
    }

    @Test
    fun `read should not touch upstream after a failed open`() {
        every { upstream.open(dataSpec) } throws IOException("HTTP 403")

        dataSource.open(dataSpec)
        dataSource.read(ByteArray(8), 0, 8)

        verify(exactly = 0) { upstream.read(any(), any(), any()) }
    }

    @Test
    fun `read should end the stream when upstream fails mid-read`() {
        every { upstream.open(dataSpec) } returns C.LENGTH_UNSET.toLong()
        every { upstream.read(any(), any(), any()) } throws IOException("connection reset")

        dataSource.open(dataSpec)

        expectThat(dataSource.read(ByteArray(8), 0, 8)).isEqualTo(C.RESULT_END_OF_INPUT)
    }

    @Test
    fun `interrupted IO should still propagate so load cancellation works`() {
        every { upstream.open(dataSpec) } throws InterruptedIOException()

        expectThrows<InterruptedIOException> { dataSource.open(dataSpec) }
    }

    @Test
    fun `a failed open should not poison the next open`() {
        every { upstream.open(dataSpec) } throws IOException("HTTP 500") andThen 10L
        every { upstream.read(any(), any(), any()) } returns 10

        dataSource.open(dataSpec)
        dataSource.close()

        expectThat(dataSource.open(dataSpec)).isEqualTo(10L)
        expectThat(dataSource.read(ByteArray(10), 0, 10)).isEqualTo(10)
    }
}
