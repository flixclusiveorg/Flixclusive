package com.flixclusive.core.presentation.player.renderer

import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.text.SubtitleDecoderFactory
import androidx.media3.extractor.text.SimpleSubtitleDecoder
import androidx.media3.extractor.text.Subtitle
import androidx.media3.extractor.text.SubtitleDecoder
import androidx.media3.extractor.text.SubtitleParser
import com.flixclusive.core.presentation.player.SubtitleOffsetProvider
import java.lang.ref.WeakReference

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
internal class CustomSubtitleDecoderFactory(
    private val offsetProvider: SubtitleOffsetProvider
) : SubtitleDecoderFactory {
    override fun supportsFormat(format: Format): Boolean {
        return listOf(
            MimeTypes.TEXT_VTT,
            MimeTypes.TEXT_SSA,
            MimeTypes.APPLICATION_TTML,
            MimeTypes.APPLICATION_MP4VTT,
            MimeTypes.APPLICATION_SUBRIP,
            MimeTypes.APPLICATION_TX3G,
            MimeTypes.APPLICATION_DVBSUBS,
            MimeTypes.APPLICATION_PGS,
        ).contains(format.sampleMimeType)
    }

    /**
     * Keep a weak reference to the latest decoder to allow access to subtitle cues
     * without preventing it from being garbage collected.
     * */
    private var latestDecoder: WeakReference<CustomDecoder>? = null

//    fun getSubtitleCues(): List<SubtitleCue>? {
//        return latestDecoder?.get()?.currentSubtitleCues
//    }

    /**
     * Decoders created here persists across reset()
     * Do not save state in the decoder which you want to reset (e.g subtitle offset)
     **/
    override fun createDecoder(format: Format): SubtitleDecoder {
        val parser = CustomDecoder(format, offsetProvider)

        latestDecoder = WeakReference(parser)

        return DelegatingSubtitleDecoder(
            name = "CustomDecoder for ${format.sampleMimeType}",
            parser = parser
        )
    }
}

/**
 * A delegating subtitle decoder that uses a [SubtitleParser] to parse subtitle data.
 *
 * This uses the old method of using the [SimpleSubtitleDecoder] instead of the new Media3 implementation
 * */
@UnstableApi
private class DelegatingSubtitleDecoder(
    name: String,
    private val parser: SubtitleParser,
) : SimpleSubtitleDecoder(name) {
    override fun decode(
        data: ByteArray,
        length: Int,
        reset: Boolean,
    ): Subtitle {
        if (reset) {
            parser.reset()
        }

        return parser.parseToLegacySubtitle(data, 0, length)
    }
}
