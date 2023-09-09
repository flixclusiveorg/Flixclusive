package com.flixclusive.presentation.mobile.main

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.flixclusive.R
import com.flixclusive.presentation.utils.ImageRequestCreator.buildImageUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenImageDialog(
    imagePath: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val posterImage = context.buildImageUrl(
        imagePath = imagePath,
        imageSize = "original"
    )

    AlertDialog(
        onDismissRequest = onDismiss
    ) {
        AsyncImage(
            model = posterImage,
            placeholder = painterResource(R.drawable.movie_placeholder),
            error = painterResource(R.drawable.movie_placeholder),
            contentDescription = stringResource(R.string.film_item_content_description),
            modifier = Modifier
                .height(500.dp)
                .fillMaxWidth()
        )
    }
}