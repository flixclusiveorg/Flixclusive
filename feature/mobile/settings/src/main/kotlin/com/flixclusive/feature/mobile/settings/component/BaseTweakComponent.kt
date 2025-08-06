package com.flixclusive.feature.mobile.settings.component

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.feature.mobile.settings.TweakPaddingHorizontal

internal val TweakTouchSize = 56.dp
internal val TweakIconSize = 25.dp

@Composable
internal fun BaseTweakComponent(
    title: String,
    modifier: Modifier = Modifier,
    descriptionProvider: (() -> String)? = null,
    enabledProvider: () -> Boolean = { true },
    onClick: (() -> Unit)? = null,
    extraContent: @Composable (() -> Unit)? = null,
    startContent: @Composable (() -> Unit)? = null,
    endContent: @Composable (() -> Unit)? = null,
) {
    val defaultHorizontalPadding = getAdaptiveDp(TweakPaddingHorizontal * 2F)
    val defaultIconWidthSpace = getAdaptiveDp(TweakIconSize)

    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier =
            modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { onClick?.invoke() },
                    enabled = enabledProvider(),
                )
                .graphicsLayer {
                    alpha = if (enabledProvider()) 1F else 0.6F
                },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(defaultHorizontalPadding),
            modifier =
                Modifier
                    .indication(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                    )
                    .padding(
                        vertical = getAdaptiveDp(10.dp),
                        horizontal = defaultHorizontalPadding,
                    )
                    .fillMaxWidth()
                    .heightIn(min = getAdaptiveDp(TweakTouchSize)),
        ) {
            if (startContent != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .width(defaultIconWidthSpace),
                ) {
                    startContent()
                }
            }

            TitleDescriptionHeader(
                title = title,
                descriptionProvider = descriptionProvider,
                modifier =
                    Modifier
                        .weight(1F)
                        .padding(end = defaultHorizontalPadding),
            )

            if (endContent != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .width(defaultIconWidthSpace),
                ) {
                    endContent()
                }
            }
        }

        if (extraContent != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .padding(horizontal = defaultHorizontalPadding),
            ) {
                extraContent()
            }
        }
    }
}
