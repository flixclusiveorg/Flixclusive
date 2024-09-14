package com.flixclusive.core.ui.common.util

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource

/**
 * An icon resource that can represent either a drawable resource ID or an image vector.
 * @param drawableId The drawable resource ID. Defaults to null.
 * @param imageVector The image vector. Defaults to null.
 */
class IconResource private constructor(
    @DrawableRes private val drawableId: Int? = null,
    private val imageVector: ImageVector? = null
) {
    /**
     * Returns the icon resource as a Compose Painter.
     * @return The Compose Painter representing the icon resource.
     */
    @Composable
    fun asPainterResource(): Painter {
        drawableId?.let {
            return painterResource(id = it)
        }

        return rememberVectorPainter(image = imageVector!!)
    }

    /**
     * A companion object providing factory methods to create IconResource instances.
     */
    companion object {
        /**
         * Creates an IconResource from a drawable resource ID.
         * @param drawableId The drawable resource ID.
         * @return The created IconResource.
         */
        fun fromDrawableResource(@DrawableRes drawableId: Int): IconResource {
            return IconResource(drawableId = drawableId)
        }

        /**
         * Creates an IconResource from an image vector.
         * @param imageVector The image vector.
         * @return The created IconResource.
         */
        fun fromImageVector(imageVector: ImageVector?): IconResource {
            return IconResource(imageVector = imageVector)
        }
    }
}
