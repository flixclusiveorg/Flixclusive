package com.flixclusive.core.ui.mobile.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.flixclusive.core.ui.common.util.onMediumEmphasis

@Composable
fun ImageWithSmallPlaceholder(
    modifier: Modifier = Modifier,
    placeholderModifier: Modifier = Modifier,
    urlImage: String?,
    @DrawableRes placeholderId: Int,
    @StringRes contentDescId: Int,
    shape: Shape = CircleShape
) {
    Surface(
        modifier = modifier,
        tonalElevation = 65.dp,
        shape = shape
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = placeholderId),
                contentDescription = stringResource(id = contentDescId),
                tint = LocalContentColor.current.onMediumEmphasis(0.8F),
                modifier = placeholderModifier
            )

            if (urlImage != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(urlImage)
                        .crossfade(true)
                        .build(),
                    imageLoader = LocalContext.current.imageLoader,
                    contentDescription = stringResource(id = contentDescId),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                )
            }
        }
    }
}