package com.flixclusive.core.presentation.player.util

import androidx.media3.common.MimeTypes
import androidx.media3.extractor.text.pgs.PgsParser
import androidx.media3.extractor.text.ssa.SsaParser
import androidx.media3.extractor.text.subrip.SubripParser
import androidx.media3.extractor.text.tx3g.Tx3gParser
import androidx.media3.extractor.text.webvtt.Mp4WebvttParser
import androidx.media3.extractor.text.webvtt.WebvttParser
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

class SubtitleFormatSnifferTest {
    @Test
    fun `sniff should detect WebVTT from its header`() {
        val result = SubtitleFormatSniffer.sniff("WEBVTT\n\n00:00:01.000 --> 00:00:02.000\nHello")

        expectThat(result).isEqualTo(MimeTypes.TEXT_VTT)
    }

    @Test
    fun `sniff should detect WebVTT even when shorter than 10 characters`() {
        // Regression: the original isWebVtt() used substring(0, 10), which threw
        // StringIndexOutOfBoundsException for any subtitle shorter than 10 characters — silently
        // producing zero cues instead of detecting the format.
        val result = SubtitleFormatSniffer.sniff("WEBVTT")

        expectThat(result).isEqualTo(MimeTypes.TEXT_VTT)
    }

    @Test
    fun `sniff should not throw on an empty string`() {
        val result = SubtitleFormatSniffer.sniff("")

        expectThat(result).isNull()
    }

    @Test
    fun `sniff should detect TTML from its XML declaration`() {
        val result = SubtitleFormatSniffer.sniff("<?xml version=\"1.0\" encoding=\"utf-8\"?><tt></tt>")

        expectThat(result).isEqualTo(MimeTypes.APPLICATION_TTML)
    }

    @Test
    fun `sniff should detect SSA from its Script Info header`() {
        val result = SubtitleFormatSniffer.sniff("[Script Info]\nTitle: Example")

        expectThat(result).isEqualTo(MimeTypes.TEXT_SSA)
    }

    @Test
    fun `sniff should detect SSA from a leading Title colon line`() {
        val result = SubtitleFormatSniffer.sniff("Title: Example\n[Script Info]")

        expectThat(result).isEqualTo(MimeTypes.TEXT_SSA)
    }

    @Test
    fun `sniff should detect SRT from its leading subtitle number`() {
        val result = SubtitleFormatSniffer.sniff("1\n00:00:01,000 --> 00:00:02,000\nHello")

        expectThat(result).isEqualTo(MimeTypes.APPLICATION_SUBRIP)
    }

    @Test
    fun `sniff should return null for unrecognized content`() {
        val result = SubtitleFormatSniffer.sniff("this is not a subtitle file")

        expectThat(result).isNull()
    }

    @Test
    fun `trimInvisible should strip leading control characters before sniffing`() {
        val result = SubtitleFormatSniffer.sniff(SubtitleFormatSniffer.trimInvisible("﻿WEBVTT"))

        expectThat(result).isEqualTo(MimeTypes.TEXT_VTT)
    }

    @Test
    fun `toParser should build the matching parser for each supported mime type`() {
        // APPLICATION_TTML is intentionally not covered here: TtmlParser's constructor calls
        // android.util.Xml's real XmlPullParserFactory, which isn't available under a plain JVM
        // unit test (RuntimeException: "not mocked"). That mapping is covered by the
        // androidTest suite instead.
        expectThat(SubtitleFormatSniffer.toParser(MimeTypes.TEXT_VTT)).isA<WebvttParser>()
        expectThat(SubtitleFormatSniffer.toParser(MimeTypes.TEXT_SSA)).isA<SsaParser>()
        expectThat(SubtitleFormatSniffer.toParser(MimeTypes.APPLICATION_MP4VTT)).isA<Mp4WebvttParser>()
        expectThat(SubtitleFormatSniffer.toParser(MimeTypes.APPLICATION_SUBRIP)).isA<SubripParser>()
        expectThat(SubtitleFormatSniffer.toParser(MimeTypes.APPLICATION_TX3G)).isA<Tx3gParser>()
        expectThat(SubtitleFormatSniffer.toParser(MimeTypes.APPLICATION_PGS)).isA<PgsParser>()
    }

    // APPLICATION_DVBSUBS -> DvbParser is intentionally untested: DvbParser's constructor
    // parses a real subtitling_descriptor out of initializationData[0], and this app never
    // produces DVB subtitles (it's dead code inherited from the codebase this was ported from —
    // see the CEA608/708 TODOs next to it in CustomSubtitleParser's original form). Fabricating
    // valid descriptor bytes just to exercise a single `when` branch isn't worth it.

    @Test
    fun `toParser should return null for an unsupported or null mime type`() {
        expectThat(SubtitleFormatSniffer.toParser(null)).isNull()
        expectThat(SubtitleFormatSniffer.toParser("application/unknown")).isNull()
    }
}
