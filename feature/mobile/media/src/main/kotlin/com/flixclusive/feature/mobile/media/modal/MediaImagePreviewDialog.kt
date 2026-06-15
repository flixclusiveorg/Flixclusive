package com.flixclusive.feature.mobile.media.modal

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.flixclusive.core.navigation.navigator.NavigateBack
import com.flixclusive.core.presentation.common.components.MediaCover
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph
import com.ramcosta.composedestinations.spec.DestinationStyle

@Destination<ExternalModuleGraph>(
    style = DestinationStyle.Dialog::class
)
@Composable
internal fun MediaImagePreviewDialog(
    imagePath: String,
    navigator: NavigateBack
) {
    MediaCoverPreviewContent(
        imagePath = imagePath,
        onDismiss = navigator::navigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaCoverPreviewContent(
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
