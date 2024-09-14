package com.flixclusive.core.ui.tv

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.imageLoader
import com.flixclusive.core.ui.common.util.buildImageUrl
import com.flixclusive.model.film.Film

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FilmLogo(
    modifier: Modifier = Modifier,
    film: Film,
    alignment: Alignment = Alignment.CenterStart,
    showTitleOnError: Boolean = true,
) {
    val context = LocalContext.current

    var isImageError by remember { mutableStateOf(false) }

    val logoImage = context.buildImageUrl(
        imagePath = film.logoImage?.replace("svg", "png"),
        imageSize = "w500"
    )

    if((isImageError || film.logoImage == null) && showTitleOnError) {
        Text(
            text = film.title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 30.sp
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            softWrap = true,
            modifier = Modifier
                .fillMaxWidth(0.45F)
        )
    } else {
        AsyncImage(
            model = logoImage,
            imageLoader = LocalContext.current.imageLoader,
            contentDescription = film.title,
            modifier = modifier,
            alignment = alignment,
            onError = { isImageError = true },
        )
    }

}