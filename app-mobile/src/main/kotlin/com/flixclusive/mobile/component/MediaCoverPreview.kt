package com.flixclusive.mobile.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.flixclusive.core.presentation.common.components.MediaCover

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MediaCoverPreview(
    imagePath: String,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        MediaCover.Poster(
            imagePath = imagePath,
            title = "",
            modifier = Modifier.fillMaxWidth()
        )
    }
}
