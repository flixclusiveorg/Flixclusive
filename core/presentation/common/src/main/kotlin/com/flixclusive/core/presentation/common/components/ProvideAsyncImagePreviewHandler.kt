package com.flixclusive.core.presentation.common.components

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler

/**
 * Provides a preview handler when using AsyncImage from Coil
 * */
@OptIn(ExperimentalCoilApi::class)
@Composable
fun ProvideAsyncImagePreviewHandler(
    color: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit,
) {
    ProvideAsyncImagePreviewHandler(
        drawable = object : Drawable() {
            private val paint = Paint().apply {
                style = Paint.Style.FILL
                this.color = color.toArgb()
            }

            override fun draw(canvas: Canvas) {
                canvas.drawRect(bounds, paint)
            }

            override fun setAlpha(alpha: Int) {
                paint.alpha = alpha
                invalidateSelf()
            }

            override fun setColorFilter(colorFilter: ColorFilter?) {
                paint.colorFilter = colorFilter
                invalidateSelf()
            }

            @Deprecated("Deprecated in Java")
            override fun getOpacity(): Int = PixelFormat.OPAQUE
        },
        content = content,
    )
}

/**
 * Provides a preview handler when using AsyncImage from Coil
 * */
@OptIn(ExperimentalCoilApi::class)
@Composable
fun ProvideAsyncImagePreviewHandler(
    drawable: Drawable?,
    content: @Composable () -> Unit,
) {
    val previewHandler = remember {
        AsyncImagePreviewHandler {
            drawable?.asImage()
        }
    }

    CompositionLocalProvider(
        LocalAsyncImagePreviewHandler provides previewHandler,
        content = content,
    )
}
