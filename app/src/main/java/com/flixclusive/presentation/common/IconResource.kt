package com.flixclusive.presentation.common

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource

// https://stackoverflow.com/a/75526761
class IconResource private constructor(
    @DrawableRes private val drawableId: Int? = null,
    private val imageVector: ImageVector? = null
) {
    @Composable
    fun asPainterResource(): Painter {
        drawableId?.let {
            return painterResource(id = it)
        }

        return rememberVectorPainter(image = imageVector!!)
    }

    companion object {
        fun fromDrawableResource(@DrawableRes drawableId: Int): IconResource {
            return IconResource(drawableId = drawableId)
        }

        fun fromImageVector(imageVector: ImageVector?): IconResource {
            return IconResource(imageVector = imageVector)
        }
    }
}