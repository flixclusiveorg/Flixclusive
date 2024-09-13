package com.flixclusive.mobile.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.flixclusive.core.ui.common.FilmCover

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilmCoverPreview(
    imagePath: String,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        FilmCover.Poster(
            imagePath = imagePath,
            imageSize = "original",
            showPlaceholder = true,
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}