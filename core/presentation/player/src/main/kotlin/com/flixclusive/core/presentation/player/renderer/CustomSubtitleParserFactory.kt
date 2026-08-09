package com.flixclusive.core.presentation.player.renderer

import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.text.DefaultSubtitleParserFactory
import androidx.media3.extractor.text.SubtitleParser
import com.flixclusive.core.presentation.player.CuesProvider

/**
 * Custom SubtitleDecoderFactory to create instances of CustomDecoder.
 *
 * This supports a customized subtitle syncer and other features.
 *
 * Massive props to Cloudstream3 for the idea and implementation of a custom subtitle decoder.
 *
 * See [SubtitleDecoderFactory](https://github.com/google/ExoPlayer/blob/release-v2/library/core/src/main/java/com/google/android/exoplayer2/text/SubtitleDecoderFactory.java)
 * */
@UnstableApi
internal class CustomSubtitleParserFactory(
    val cuesProvider: CuesProvider,
    val delegate: SubtitleParser.Factory = DefaultSubtitleParserFactory()
) : SubtitleParser.Factory by delegate {
    private val supportedMimeTypes by lazy {
        setOf(
            MimeTypes.TEXT_VTT,
            MimeTypes.TEXT_SSA,
            MimeTypes.APPLICATION_TTML,
            MimeTypes.APPLICATION_MP4VTT,
            MimeTypes.APPLICATION_SUBRIP,
            MimeTypes.APPLICATION_TX3G,
            MimeTypes.APPLICATION_DVBSUBS,
            MimeTypes.APPLICATION_PGS,
        )
    }

    override fun supportsFormat(format: Format): Boolean {
        return supportedMimeTypes.contains(format.sampleMimeType)
    }

    override fun getCueReplacementBehavior(format: Format): Int {
        return delegate.getCueReplacementBehavior(format)
    }

    override fun create(format: Format): SubtitleParser {
        return CustomSubtitleParser(format, cuesProvider)
    }
}
