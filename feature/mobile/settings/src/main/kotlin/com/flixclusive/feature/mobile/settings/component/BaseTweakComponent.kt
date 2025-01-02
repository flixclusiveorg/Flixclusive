package com.flixclusive.feature.mobile.settings.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.feature.mobile.settings.TweakPaddingHorizontal

internal val TweakTouchSize = 56.dp
internal val TweakIconSize = 35.dp

@Composable
internal fun BaseTweakComponent(
    modifier: Modifier = Modifier,
    title: String,
    description: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    extraContent: @Composable (() -> Unit)? = null,
    startContent: @Composable (() -> Unit)? = null,
    endContent: @Composable (() -> Unit)? = null,
) {
    val defaultHorizontalPadding = getAdaptiveDp(TweakPaddingHorizontal * 2F)
    val defaultIconWidthSpace = getAdaptiveDp(TweakIconSize)

    val alpha by animateFloatAsState(
        label = "alpha",
        targetValue = if (enabled) 1F else 0.6F,
    )

    Column(
        modifier = (if (extraContent != null) modifier else Modifier)
            .alpha(alpha),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(defaultHorizontalPadding),
            modifier = (if (extraContent == null) modifier else Modifier)
                .clickable(
                    enabled = onClick != null && enabled,
                    onClick = { onClick?.invoke() }
                )
                .padding(
                    vertical = getAdaptiveDp(10.dp),
                    horizontal = defaultHorizontalPadding
                )
                .fillMaxWidth()
                .heightIn(min = getAdaptiveDp(TweakTouchSize))
        ) {
            if (startContent != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(defaultIconWidthSpace)
                ) {
                    startContent()
                }
            }

            GroupLabel(
                title = title,
                description = description,
                modifier = Modifier.weight(1F)
            )

            if (endContent != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(defaultIconWidthSpace)
                ) {
                    endContent()
                }
            }
        }

        if (extraContent != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(horizontal = defaultHorizontalPadding)
            ) {
                extraContent()
            }
        }
    }
}