package com.flixclusive.core.presentation.player.util

import androidx.annotation.OptIn
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.text.SubtitleParser
import androidx.media3.extractor.text.dvb.DvbParser
import androidx.media3.extractor.text.pgs.PgsParser
import androidx.media3.extractor.text.ssa.SsaParser
import androidx.media3.extractor.text.subrip.SubripParser
import androidx.media3.extractor.text.ttml.TtmlParser
import androidx.media3.extractor.text.tx3g.Tx3gParser
import androidx.media3.extractor.text.webvtt.Mp4WebvttParser
import androidx.media3.extractor.text.webvtt.WebvttParser

/**
 * Detects a subtitle's actual format from its decoded text content, and builds the matching
 * Media3 [SubtitleParser] for it.
 *
 * Shared by [com.flixclusive.core.presentation.player.renderer.CustomSubtitleParser], which has
 * real bytes to sniff, and by [MimeTypeParser], which only has a URL/filename to guess from
 * before any bytes are read — that pre-playback guess only needs to be *supported*, since a wrong
 * one self-corrects here once the file is actually opened.
 */
@OptIn(UnstableApi::class)
internal object SubtitleFormatSniffer {
    /**
     * Strips invisible characters (control/formatting chars) from the start of [text] so format
     * detection isn't thrown off by a BOM or similar leading noise.
     */
    fun trimInvisible(text: String): String {
        val controlCharsRegex = Regex("""[\p{Cntrl}\p{Cf}]""")
        return text.trimStart { it.isWhitespace() || controlCharsRegex.matches(it.toString()) }
    }

    /**
     * Detects the subtitle format from its (already [trimInvisible]-d) decoded text.
     *
     * @return a [MimeTypes] constant, or null if none of the known formats match.
     */
    fun sniff(trimmedText: String): String? = when {
        trimmedText.isWebVtt() -> MimeTypes.TEXT_VTT
        trimmedText.isTtml() -> MimeTypes.APPLICATION_TTML
        trimmedText.isSsa() -> MimeTypes.TEXT_SSA
        trimmedText.isSrt() -> MimeTypes.APPLICATION_SUBRIP
        else -> null
    }

    /** Builds the Media3 parser for a [MimeTypes] constant, or null if [mimeType] is null or unsupported. */
    fun toParser(
        mimeType: String?,
        initializationData: List<ByteArray>? = null,
    ): SubtitleParser? = when (mimeType) {
        MimeTypes.TEXT_VTT -> WebvttParser()
        MimeTypes.TEXT_SSA -> SsaParser(initializationData)
        MimeTypes.APPLICATION_MP4VTT -> Mp4WebvttParser()
        MimeTypes.APPLICATION_TTML -> TtmlParser()
        MimeTypes.APPLICATION_SUBRIP -> SubripParser()
        MimeTypes.APPLICATION_TX3G -> Tx3gParser(initializationData.orEmpty())
        MimeTypes.APPLICATION_DVBSUBS -> DvbParser(initializationData.orEmpty())
        MimeTypes.APPLICATION_PGS -> PgsParser()
        // TODO: These decoders are not converted to parsers yet
//      MimeTypes.APPLICATION_CEA608, MimeTypes.APPLICATION_MP4CEA608 -> Cea608Decoder(...)
//      MimeTypes.APPLICATION_CEA708 -> Cea708Decoder(...)
        else -> null
    }

    /**
     * Checks only the first 10 characters, to avoid issues with a BOM or invisible characters
     * at the start of the file. Uses [take] rather than `substring(0, 10)` — the original crashed
     * with [StringIndexOutOfBoundsException] on any subtitle shorter than 10 characters.
     */
    private fun String.isWebVtt(): Boolean = take(10).contains("WEBVTT", ignoreCase = true)

    /** TTML files are XML, so they should start with the XML declaration. */
    private fun String.isTtml(): Boolean = startsWith("<?xml version=\"", ignoreCase = true)

    /** SSA/ASS files usually start with the [Script Info] header or a Title: line. */
    private fun String.isSsa(): Boolean =
        startsWith("[Script Info]", ignoreCase = true) || startsWith("Title:", ignoreCase = true)

    /** SRT files start with the first subtitle number. */
    private fun String.isSrt(): Boolean = startsWith("1", ignoreCase = true)
}
