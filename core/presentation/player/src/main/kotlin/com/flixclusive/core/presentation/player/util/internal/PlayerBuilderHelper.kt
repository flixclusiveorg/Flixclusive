package com.flixclusive.core.presentation.player.util.internal

import android.content.Context
import android.os.Handler
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
import androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
import androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.metadata.MetadataOutput
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.text.TextOutput
import androidx.media3.exoplayer.text.TextRenderer
import androidx.media3.exoplayer.video.VideoRendererEventListener
import com.flixclusive.core.datastore.model.user.player.DecoderPriority
import com.flixclusive.core.presentation.player.SubtitleOffsetProvider
import com.flixclusive.core.presentation.player.renderer.CustomSubtitleDecoderFactory
import com.flixclusive.core.util.network.okhttp.SSLTrustManager
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory
import java.security.SecureRandom
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import kotlin.math.max
import kotlin.math.min

internal object PlayerBuilderHelper {
    /**
     * Obtains an array of [Renderer] instances configured with the specified parameters.
     * */
    @OptIn(UnstableApi::class)
    fun Context.getRenderers(
        eventHandler: Handler,
        videoRendererEventListener: VideoRendererEventListener,
        audioRendererEventListener: AudioRendererEventListener,
        textRendererOutput: TextOutput,
        metadataRendererOutput: MetadataOutput,
        subtitleOffsetProvider: SubtitleOffsetProvider,
        decoderPriority: DecoderPriority,
        onTextRendererChange: (TextRenderer) -> Unit,
    ): Array<Renderer> {
        return NextRenderersFactory(this)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(
                when (decoderPriority) {
                    DecoderPriority.DEVICE_ONLY -> EXTENSION_RENDERER_MODE_OFF
                    DecoderPriority.PREFER_DEVICE -> EXTENSION_RENDERER_MODE_ON
                    DecoderPriority.PREFER_APP -> EXTENSION_RENDERER_MODE_PREFER
                },
            ).createRenderers(
                eventHandler,
                videoRendererEventListener,
                audioRendererEventListener,
                textRendererOutput,
                metadataRendererOutput,
            ).map {
                if (it is TextRenderer) {
                    // Pass subtitle offset provider directly to the decoder factory
                    val decoder = CustomSubtitleDecoderFactory(subtitleOffsetProvider)

                    val currentTextRenderer = TextRenderer(
                        textRendererOutput,
                        eventHandler.looper,
                        decoder,
                    ).apply {
                        // Required to make the decoder work with old subtitles
                        // Upgrade CustomSubtitleDecoderFactory when media3 supports it
                        @Suppress("DEPRECATION")
                        experimentalSetLegacyDecodingEnabled(true)
                    }

                    onTextRendererChange(currentTextRenderer)
                    currentTextRenderer
                } else {
                    it
                }
            }.toTypedArray()
    }

    /**
     * Creates and configures a [DefaultLoadControl] instance with the specified buffer sizes.
     * */
    @OptIn(UnstableApi::class)
    fun getLoadControl(
        bufferCacheSize: Long,
        videoBufferMs: Long,
    ): DefaultLoadControl {
        val targetBufferBytes = min(Int.MAX_VALUE, max(bufferCacheSize.toInt() * 1024 * 1024, C.LENGTH_UNSET))

        val targetBufferMs = videoBufferMs.toInt() * 1000

        return DefaultLoadControl
            .Builder()
            .setTargetBufferBytes(targetBufferBytes)
            .setBackBuffer(
                // backBufferDurationMs =
                30_000,
                // retainBackBufferFromKeyframe =
                true,
            ).setBufferDurationsMs(
                // minBufferMs =
                targetBufferMs,
                // maxBufferMs =
                targetBufferMs,
                // bufferForPlaybackMs =
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                // bufferForPlaybackAfterRebufferMs =
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
            ).build()
    }

    /**
     * Creates a [DefaultMediaSourceFactory] that uses caching for media data.
     * */
    @OptIn(UnstableApi::class)
    fun Context.getCacheFactory(
        cache: Cache,
        onlineDataSource: DataSource.Factory,
    ): DefaultMediaSourceFactory {
        val cacheFactory = CacheDataSource.Factory().apply {
            cache.let(::setCache)
            setUpstreamDataSourceFactory(onlineDataSource)
        }

        return DefaultMediaSourceFactory(this)
            .setDataSourceFactory(cacheFactory)
    }

    /**
     * Disables ssl verification
     * */
    fun disableSSLVerification() {
        val sslContext: SSLContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(SSLTrustManager()), SecureRandom())
        sslContext.createSSLEngine()
        HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
    }
}
