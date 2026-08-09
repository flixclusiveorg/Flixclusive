package com.flixclusive.core.presentation.player.renderer

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorInput
import androidx.media3.extractor.ExtractorOutput
import androidx.media3.extractor.PositionHolder
import androidx.media3.extractor.SeekMap
import java.io.IOException

@UnstableApi
internal class UnknownSubtitlesExtractor(private val format: Format) : Extractor {
    override fun sniff(input: ExtractorInput): Boolean {
        return true
    }

    override fun init(output: ExtractorOutput) {
        val trackOutput = output.track(0, C.TRACK_TYPE_TEXT)
        output.seekMap(SeekMap.Unseekable(C.TIME_UNSET))
        output.endTracks()
        trackOutput.format(
            format
                .buildUpon()
                .setSampleMimeType(MimeTypes.TEXT_UNKNOWN)
                .setCodecs(format.sampleMimeType)
                .build()
        )
    }

    @Throws(IOException::class)
    override fun read(input: ExtractorInput, seekPosition: PositionHolder): Int {
        val skipResult = input.skip(Int.MAX_VALUE)
        if (skipResult == C.RESULT_END_OF_INPUT) {
            return Extractor.RESULT_END_OF_INPUT
        }
        return Extractor.RESULT_CONTINUE
    }

    override fun seek(position: Long, timeUs: Long) {}

    override fun release() {}
}
