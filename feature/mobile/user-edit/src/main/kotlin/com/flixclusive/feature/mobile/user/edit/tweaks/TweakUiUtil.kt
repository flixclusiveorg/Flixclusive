package com.flixclusive.feature.mobile.user.edit.tweaks

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.unit.dp
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.isExpanded

internal object TweakUiUtil {
    val DefaultShape = RoundedCornerShape(4.0.dp)

    fun Modifier.fillMaxAdaptiveWidth(): Modifier = composed {
        val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
        val windowWidthSizeClass = windowSizeClass.windowWidthSizeClass
        val fraction = when {
            windowWidthSizeClass.isExpanded -> 0.6F
            else -> 1F
        }

        return@composed fillMaxWidth(fraction)
    }
}
