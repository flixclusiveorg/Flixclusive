package com.flixclusive.core.presentation.common.components

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.imageLoader
import com.flixclusive.core.presentation.common.components.FilmCover.Backdrop
import com.flixclusive.core.presentation.common.components.FilmCover.Poster
import com.flixclusive.core.presentation.common.extensions.buildImageRequest
import com.flixclusive.core.presentation.common.extensions.ifElse

/**
 * Enum representing different types of film covers with their respective aspect ratios.
 *
 * - [Backdrop]: Typically has an aspect ratio of 16:9.
 * - [Poster]: Typically has an aspect ratio of 2:3.
 *
 * @property ratio The aspect ratio of the film cover.
 * */
enum class FilmCover(
    val ratio: Float,
) {
    /** An image with an aspect ratio of 16:9 */
    Backdrop(16F / 9F),

    /** An image with an aspect ratio of 2:3 */
    Poster(2F / 3F),
    ;

    /**
     * Composable that displays a film cover image (poster or backdrop) with a placeholder.
     *
     * @param imagePath The path to the image to be loaded.
     * @param imageSize The size of the image to be loaded (e.g., "w500").
     * @param title The title of the film, used for the placeholder.
     * @param modifier Modifier to be applied to the Box containing the image.
     * @param onSuccess Optional lambda to be invoked when the image has been successfully loaded
     * @param onClick Optional lambda to be invoked when the image is clicked.
     * @param onLongClick Optional lambda to be invoked when the image is long-clicked
     * */
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    operator fun invoke(
        imagePath: String?,
        imageSize: String,
        title: String,
        modifier: Modifier = Modifier,
        onSuccess: () -> Unit = {},
        onClick: (() -> Unit)? = null,
        onLongClick: (() -> Unit)? = null,
    ) {
        val haptic = LocalHapticFeedback.current
        val context = LocalContext.current

        val painter = remember(imagePath) {
            context.buildImageRequest(
                imagePath = imagePath,
                imageSize = imageSize,
            )
        }

        AsyncImage(
            model = painter,
            imageLoader = LocalContext.current.imageLoader,
            contentScale = ContentScale.FillBounds,
            contentDescription = title,
            onSuccess = { onSuccess() },
            modifier = modifier
                .aspectRatio(ratio)
                .ifElse(
                    condition = onClick != null,
                    ifTrueModifier =
                        Modifier.combinedClickable(
                            onClick = { onClick?.invoke() },
                            onLongClick = {
                                onLongClick?.let {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    it.invoke()
                                }
                            },
                        ),
                ),
        )
    }
}

@SuppressLint("UseCompatLoadingForDrawables")
@Preview
@Composable
private fun FilmCoverPreview() {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            surface = Color(0xFF121212),
            onSurface = Color(0xFFFFFFFF),
            surfaceTint = Color(0xFFFFFFFF),
        ),
    ) {
        Surface {
            ProvideAsyncImagePreviewHandler(
                color = MaterialTheme.colorScheme.surface,
            ) {
                Poster(
                    imagePath = "/sample.png",
                    imageSize = "w500",
                    title = "Superman (1996)",
                    modifier = Modifier.width(110.dp),
                )
            }
        }
    }
}
