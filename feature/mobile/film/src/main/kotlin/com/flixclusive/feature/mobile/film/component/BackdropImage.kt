package com.flixclusive.feature.mobile.film.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.imageLoader
import com.flixclusive.core.presentation.common.extensions.buildTMDBImageUrl
import com.flixclusive.core.presentation.common.util.SolidColorPainter
import com.flixclusive.core.presentation.mobile.extensions.isCompact
import com.flixclusive.core.presentation.mobile.extensions.isMedium
import com.flixclusive.model.film.Film

@Composable
internal fun BackdropImage(
    metadata: Film,
    modifier: Modifier = Modifier,
) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val usePortraitView = windowSizeClass.windowWidthSizeClass.isCompact ||
        windowSizeClass.windowWidthSizeClass.isMedium

    val backgroundColor = MaterialTheme.colorScheme.background
    val placeholder = SolidColorPainter.from(MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp))

    val context = LocalContext.current
    val model = remember(metadata, usePortraitView) {
        val imagePath = if (usePortraitView) metadata.posterImage else metadata.backdropImage
        val imageSize = if (usePortraitView) "original" else "w1920_and_h600_multi_faces"

        context.buildTMDBImageUrl(
            imagePath = imagePath ?: metadata.posterImage,
            imageSize = imageSize,
        )
    }

    AsyncImage(
        model = model,
        imageLoader = context.imageLoader,
        placeholder = placeholder,
        error = placeholder,
        contentDescription = metadata.title,
        contentScale = ContentScale.Crop,
        alignment = Alignment.TopCenter,
        modifier = modifier
            .fillMaxWidth()
            .drawWithContent {
                drawContent()
                drawRect(
                    Brush.verticalGradient(
                        0F to Color.Transparent,
                        0.9F to backgroundColor,
                    ),
                )
            },
    )
}
