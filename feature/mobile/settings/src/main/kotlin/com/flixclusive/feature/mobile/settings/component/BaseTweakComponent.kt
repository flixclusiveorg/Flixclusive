package com.flixclusive.feature.mobile.settings.component

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp

internal val TweakTouchSize = 56.dp
internal val TweakIconSize = 35.dp

@Composable
internal fun BaseTweakComponent(
    modifier: Modifier = Modifier,
    title: String,
    description: String? = null,
    onClick: (() -> Unit)? = null,
    extraContent: @Composable (() -> Unit)? = null,
    startContent: @Composable (() -> Unit)? = null,
    endContent: @Composable (() -> Unit)? = null,
) {
    val defaultIconWidthSpace = getAdaptiveDp(TweakIconSize)

    Column(
        modifier = (if (extraContent != null) modifier else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = (if (extraContent == null) modifier else Modifier)
                .fillMaxWidth()
                .heightIn(min = getAdaptiveDp(TweakTouchSize))
                .clickable(
                    enabled = onClick != null,
                    onClick = { onClick?.invoke() }
                )
                .padding(vertical = getAdaptiveDp(10.dp))
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

        extraContent?.invoke()
    }
}