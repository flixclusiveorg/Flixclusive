package com.flixclusive.presentation.main

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.flixclusive.R
import com.flixclusive.presentation.common.IconResource
import com.flixclusive.presentation.common.ImageRequestCreator.buildImageUrl
import com.flixclusive.presentation.common.UiText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenImageDialog(
    imagePath: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val posterImage: ImageRequest = context.buildImageUrl(
        imagePath = imagePath,
        imageSize = "original"
    )

    AlertDialog(
        onDismissRequest = onDismiss
    ) {
        AsyncImage(
            model = posterImage,
            placeholder = IconResource.fromDrawableResource(R.drawable.movie_placeholder)
                .asPainterResource(),
            contentDescription = UiText.StringResource(R.string.film_item_content_description)
                .asString(),
            modifier = Modifier
                .height(500.dp)
                .fillMaxWidth()
        )
    }
}