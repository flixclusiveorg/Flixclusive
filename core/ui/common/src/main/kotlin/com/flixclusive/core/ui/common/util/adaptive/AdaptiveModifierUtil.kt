package com.flixclusive.core.ui.common.util.adaptive

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.isCompact
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.isExpanded
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.isMedium

object AdaptiveModifierUtil {
    fun Modifier.fillMaxAdaptiveWidth(
        compact: Float = 1F,
        medium: Float = (compact - 0.2F),
        expanded: Float = (medium - 0.2F),
    ): Modifier = composed {
        val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
        val windowWidthSizeClass = windowSizeClass.windowWidthSizeClass
        val windowHeightSizeClass = windowSizeClass.windowHeightSizeClass
        val fraction = when {
            windowWidthSizeClass.isCompact || windowHeightSizeClass.isCompact ->  compact
            windowWidthSizeClass.isMedium || windowHeightSizeClass.isMedium -> medium
            windowWidthSizeClass.isExpanded -> expanded
            else -> compact
        }

        return@composed fillMaxWidth(fraction)
    }
}