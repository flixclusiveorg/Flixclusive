package com.flixclusive.core.ui.player.renderer

import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.text.SubtitleDecoderFactory
import androidx.media3.exoplayer.text.TextOutput

/**
 *
 * Based on: [Cloudstream TextRenderer](https://github.com/recloudstream/cloudstream/blob/master/app/src/main/java/com/lagradost/cloudstream3/ui/player/CustomTextRenderer.kt)
 *
 */
@OptIn(UnstableApi::class)
class CustomTextRenderer(
    offset: Long,
    output: TextOutput?,
    outputLooper: Looper?,
    decoderFactory: SubtitleDecoderFactory = SubtitleDecoderFactory.DEFAULT
) : NonFinalTextRenderer(output, outputLooper, decoderFactory) {
    private var offsetPositionUs: Long = 0L

    init {
        setRenderOffsetMs(offset)
    }

    fun setRenderOffsetMs(offset : Long) {
        offsetPositionUs = offset * 1000L
    }

    fun getRenderOffsetMs() : Long {
        return offsetPositionUs / 1000L
    }

    override fun render( positionUs: Long,  elapsedRealtimeUs: Long) {
        super.render(positionUs + offsetPositionUs, elapsedRealtimeUs + offsetPositionUs)
    }
}