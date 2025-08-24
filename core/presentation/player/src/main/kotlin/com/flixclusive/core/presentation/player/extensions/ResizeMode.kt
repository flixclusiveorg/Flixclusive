package com.flixclusive.core.presentation.player.extensions

import androidx.compose.ui.layout.ContentScale
import com.flixclusive.core.datastore.model.user.player.ResizeMode

internal fun ResizeMode.toContentScale(): ContentScale
    = when (this) {
    ResizeMode.Fit -> ContentScale.Fit
    ResizeMode.Crop -> ContentScale.Crop
    ResizeMode.None -> ContentScale.None
    ResizeMode.Inside -> ContentScale.Inside
    ResizeMode.Fill -> ContentScale.FillBounds
    ResizeMode.FillHeight -> ContentScale.FillHeight
    ResizeMode.FillWidth -> ContentScale.FillWidth
}
