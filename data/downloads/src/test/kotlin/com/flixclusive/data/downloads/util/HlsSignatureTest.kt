package com.flixclusive.data.downloads.util

import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class HlsSignatureTest {
    @Test
    fun `matchesUrl should accept m3u8 and m3u paths regardless of case`() {
        expectThat(HlsSignature.matchesUrl("https://cdn.example.com/master.m3u8")).isTrue()
        expectThat(HlsSignature.matchesUrl("https://cdn.example.com/master.M3U8")).isTrue()
        expectThat(HlsSignature.matchesUrl("https://cdn.example.com/master.m3u")).isTrue()
    }

    @Test
    fun `matchesUrl should ignore query strings and fragments`() {
        expectThat(HlsSignature.matchesUrl("https://cdn.example.com/master.m3u8?token=abc&e=123")).isTrue()
        expectThat(HlsSignature.matchesUrl("https://cdn.example.com/master.m3u8#t=10")).isTrue()
    }

    @Test
    fun `matchesUrl should reject a progressive file url`() {
        expectThat(HlsSignature.matchesUrl("https://cdn.example.com/movie.mp4")).isFalse()
        expectThat(HlsSignature.matchesUrl("https://cdn.example.com/m3u8/movie.mp4")).isFalse()
    }

    @Test
    fun `matchesContentType should accept every mpegurl spelling and ignore parameters`() {
        val accepted = listOf(
            "application/vnd.apple.mpegurl",
            "application/x-mpegurl",
            "application/mpegurl",
            "audio/mpegurl",
            "audio/x-mpegurl",
            "vnd.apple.mpegurl",
            "APPLICATION/X-MPEGURL; charset=utf-8",
        )

        accepted.forEach { contentType ->
            expectThat(HlsSignature.matchesContentType(contentType)).isTrue()
        }
    }

    @Test
    fun `matchesContentType should reject generic and video types`() {
        expectThat(HlsSignature.matchesContentType("video/mp4")).isFalse()
        expectThat(HlsSignature.matchesContentType("application/octet-stream")).isFalse()
        expectThat(HlsSignature.matchesContentType(null)).isFalse()
    }

    @Test
    fun `matchesBody should accept a plain manifest`() {
        expectThat(HlsSignature.matchesBody("#EXTM3U\n#EXT-X-VERSION:3\n".toByteArray())).isTrue()
    }

    @Test
    fun `matchesBody should accept a manifest behind a utf-8 bom`() {
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val body = bom + "#EXTM3U\n#EXT-X-VERSION:3\n".toByteArray()

        expectThat(HlsSignature.matchesBody(body)).isTrue()
    }

    @Test
    fun `matchesBody should accept a manifest behind leading whitespace`() {
        expectThat(HlsSignature.matchesBody("\n\r\n  #EXTM3U\n".toByteArray())).isTrue()
    }

    @Test
    fun `matchesBody should reject a buffer too short to hold the header`() {
        expectThat(HlsSignature.matchesBody("#EXTM".toByteArray())).isFalse()
    }

    @Test
    fun `matchesBody should only read up to the given length`() {
        val body = "#EXTM3U\n".toByteArray() + ByteArray(64)

        expectThat(HlsSignature.matchesBody(body, length = 4)).isFalse()
        expectThat(HlsSignature.matchesBody(body, length = 8)).isTrue()
    }

    @Test
    fun `matchesBody should reject media bytes`() {
        expectThat(HlsSignature.matchesBody(byteArrayOf(0x47, 0x40, 0x00, 0x10, 0x00))).isFalse()
    }
}
