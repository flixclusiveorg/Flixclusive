package com.flixclusive.presentation.mobile.main.composables

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.flixclusive.presentation.common.composables.FilmCover

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilmCoverPreview(
    imagePath: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss
    ) {
        FilmCover.Poster(
            imagePath = imagePath,
            imageSize = "original",
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}