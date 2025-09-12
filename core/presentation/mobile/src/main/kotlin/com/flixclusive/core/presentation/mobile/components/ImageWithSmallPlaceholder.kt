package com.flixclusive.core.presentation.mobile.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.crossfade

@Composable
fun ImageWithSmallPlaceholder(
    urlImage: String?,
    @DrawableRes placeholderId: Int,
    @StringRes contentDescId: Int,
    modifier: Modifier = Modifier,
    placeholderSize: Dp = Dp.Unspecified,
    shape: Shape = CircleShape
) {
    ImageWithSmallPlaceholder(
        model = ImageRequest.Builder(LocalContext.current)
            .data(urlImage)
            .crossfade(true)
            .build(),
        placeholder = painterResource(id = placeholderId),
        contentDescription = stringResource(id = contentDescId),
        modifier = modifier,
        placeholderSize = placeholderSize,
        shape = shape
    )
}

@Composable
fun ImageWithSmallPlaceholder(
    model: ImageRequest?,
    placeholder: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    placeholderSize: Dp = Dp.Unspecified,
    shape: Shape = CircleShape
) {
    var isSuccess by remember { mutableStateOf(false) }

    val background by animateColorAsState(
        targetValue = if (isSuccess) Color.Transparent else MaterialTheme.colorScheme.surface,
        label = "",
    )

    Surface(
        modifier = modifier,
        tonalElevation = 65.dp,
        color = background,
        shape = shape
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = !isSuccess,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Icon(
                    painter = placeholder,
                    contentDescription = contentDescription,
                    tint = LocalContentColor.current.copy(0.8F),
                    modifier = if (placeholderSize != Dp.Unspecified) {
                        Modifier.size(placeholderSize)
                    } else Modifier
                )
            }

            AsyncImage(
                model = model,
                imageLoader = LocalContext.current.imageLoader,
                contentDescription = contentDescription,
                contentScale = contentScale,
                onState = { isSuccess = it is AsyncImagePainter.State.Success },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
