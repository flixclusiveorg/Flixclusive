package com.flixclusive.core.ui.common.util

import android.content.Context
import android.graphics.Paint
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import coil3.Canvas
import coil3.Image
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import coil3.request.ImageRequest
import coil3.request.crossfade

private const val TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"

object CoilUtil {
    fun Context.buildImageUrl(
        imagePath: String?,
        imageSize: String = "w500",
    ): ImageRequest? {
        if(imagePath == null)
            return null

        val imageRequest = ImageRequest.Builder(this)
        val pattern = "(https?://.+?/p/)([^/]+)(/.+)".toRegex()

        imageRequest.apply {
            data(
                if (imagePath.isEmpty()) {
                    null
                } else if (pattern.matches(imagePath)) {
                    val replacedUrl = pattern.replace(imagePath) { matchResult ->
                        val originalString = matchResult.groupValues[2]
                        matchResult.value.replace(originalString, imageSize)
                    }
                    replacedUrl
                } else if (imagePath.startsWith("/")) {
                    "$TMDB_IMAGE_BASE_URL$imageSize$imagePath"
                } else imagePath
            )
        }

        return imageRequest
            .crossfade(true)
            .build()
    }

    @OptIn(ExperimentalCoilApi::class)
    @Composable
    fun ProvideAsyncImagePreviewHandler(
        color: Color = MaterialTheme.colorScheme.primary,
        content: @Composable () -> Unit
    ) {
        val previewHandler = AsyncImagePreviewHandler {
            object : Image {
                private var lazyPaint: Paint? = null
                override val width: Int = 100
                override val height: Int = 100
                override val size: Long = 4L * width * height
                override val shareable: Boolean = true

                override fun draw(canvas: Canvas) {
                    val paint = lazyPaint ?: run {
                        Paint()
                            .apply { this@apply.color = color.toArgb() }
                            .also { lazyPaint = it }
                    }

                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                }
            }
        }

        CompositionLocalProvider(
            LocalAsyncImagePreviewHandler provides previewHandler,
            content = content
        )
    }
}
