package com.flixclusive.presentation.common.player.utils

import android.content.Context
import android.os.Handler
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.metadata.MetadataOutput
import androidx.media3.exoplayer.text.TextOutput
import androidx.media3.exoplayer.text.TextRenderer
import androidx.media3.exoplayer.video.VideoRendererEventListener
import com.flixclusive.presentation.common.player.renderer.CustomTextRenderer
import com.flixclusive.providers.utils.network.OkHttpUtils
import com.flixclusive.utils.LoggerUtils.errorLog
import java.io.File
import java.security.SecureRandom
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import kotlin.math.min

internal object PlayerBuilderUtils {
    @OptIn(UnstableApi::class)
    fun Context.getRenderers(
        eventHandler: Handler,
        videoRendererEventListener: VideoRendererEventListener,
        audioRendererEventListener: AudioRendererEventListener,
        textRendererOutput: TextOutput,
        metadataRendererOutput: MetadataOutput,
        subtitleOffset: Long,
        onTextRendererChange: (CustomTextRenderer) -> Unit,
    ): Array<Renderer> {
        return DefaultRenderersFactory(this)
            .createRenderers(
                eventHandler,
                videoRendererEventListener,
                audioRendererEventListener,
                textRendererOutput,
                metadataRendererOutput
            ).map {
                if (it is TextRenderer) {
                    CustomTextRenderer(
                        subtitleOffset,
                        textRendererOutput,
                        eventHandler.looper,
                    ).also { renderer ->
                        onTextRendererChange(renderer)
                    }
                } else it
            }.toTypedArray()
    }

    @OptIn(UnstableApi::class)
    fun getLoadControl(
        bufferCacheSize: Long,
        videoBufferMs: Long,
    ): DefaultLoadControl {
        val targetBufferBytes = min(Int.MAX_VALUE, bufferCacheSize.toInt())
        val maxBufferMs = videoBufferMs.toInt()

        return DefaultLoadControl.Builder()
            .setTargetBufferBytes(targetBufferBytes)
            .setBackBuffer(
                /* backBufferDurationMs = */ 30_000,
                /* retainBackBufferFromKeyframe = */ true
            )
            .setBufferDurationsMs(
                /* minBufferMs = */ DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                /* maxBufferMs = */ maxBufferMs,
                /* bufferForPlaybackMs = */ DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                /* bufferForPlaybackAfterRebufferMs = */ DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            ).build()
    }

    @OptIn(UnstableApi::class)
    fun Context.getCache(cacheSize: Long): SimpleCache? {
        return try {
            val databaseProvider = StandaloneDatabaseProvider(this)
            SimpleCache(
                File(
                    this.cacheDir, "flixclusive_player"
                ).also { it.deleteOnExit() }, // Ensures always fresh file
                LeastRecentlyUsedCacheEvictor(cacheSize),
                databaseProvider
            )
        } catch (e: Exception) {
            errorLog(e.stackTraceToString())
            null
        }
    }

    /**
     *
     * Disables ssl verification
     *
     * https://github.com/recloudstream/cloudstream/blob/484c21cc1ce7d75fd6582fe9cb404cfb92af4e1f/app/src/main/java/com/lagradost/cloudstream3/ui/player/CS3IPlayer.kt#L1313
     * */
    fun disableSSLVerification() {
        val sslContext: SSLContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(OkHttpUtils.SSLTrustManager()), SecureRandom())
        sslContext.createSSLEngine()
        HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
    }
}