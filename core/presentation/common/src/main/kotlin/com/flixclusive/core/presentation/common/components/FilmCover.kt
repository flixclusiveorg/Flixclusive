package com.flixclusive.core.presentation.common.components

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.imageLoader
import com.flixclusive.core.presentation.common.components.FilmCover.Backdrop
import com.flixclusive.core.presentation.common.components.FilmCover.Poster
import com.flixclusive.core.presentation.common.extensions.buildTMDBImageUrl
import com.flixclusive.core.presentation.common.extensions.ifElse
import com.flixclusive.core.drawables.R as UiCommonR

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
        onClick: (() -> Unit)? = null,
        onLongClick: (() -> Unit)? = null,
    ) {
        var showPlaceholder by remember { mutableStateOf(true) }

        val haptic = LocalHapticFeedback.current
        val context = LocalContext.current

        val painter = remember(imagePath) {
            context.buildTMDBImageUrl(
                imagePath = imagePath,
                imageSize = imageSize,
            )
        }

        Box(
            modifier = modifier.aspectRatio(ratio)
        ) {
            AsyncImage(
                model = painter,
                imageLoader = LocalContext.current.imageLoader,
                contentScale = ContentScale.FillBounds,
                contentDescription = title,
                onSuccess = { showPlaceholder = false },
                modifier = Modifier
                    .matchParentSize()
                    .align(Alignment.Center)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                        shape = MaterialTheme.shapes.extraSmall
                    )
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

            if (showPlaceholder) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.matchParentSize()
                ) {
                    Box(
                        contentAlignment = Alignment.BottomCenter,
                        modifier = Modifier.weight(0.4f)
                    ) {
                        Icon(
                            painter = painterResource(id = UiCommonR.drawable.movie_icon),
                            contentDescription = "Superman (1996)",
                            tint = LocalContentColor.current.copy(0.6f),
                            modifier = Modifier
                                .size(40.dp)
                        )
                    }

                    Box(
                        contentAlignment = Alignment.TopCenter,
                        modifier = Modifier.weight(0.6F)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis,
                            color = LocalContentColor.current.copy(0.6f),
                            modifier = Modifier
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
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
            surfaceTint = Color(0xFFFFFFFF)
        )
    ) {
        Surface {
            ProvideAsyncImagePreviewHandler(
                color = MaterialTheme.colorScheme.surface
            ) {
                Poster(
                    imagePath = "/sample.png",
                    imageSize = "w500",
                    title = "Superman (1996)",
                    modifier = Modifier.width(110.dp)
                )
            }
        }
    }
}
