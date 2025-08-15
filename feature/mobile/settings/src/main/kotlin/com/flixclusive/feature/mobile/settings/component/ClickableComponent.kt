package com.flixclusive.feature.mobile.settings.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.flixclusive.core.presentation.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.adaptive.AdaptiveIcon
import com.flixclusive.core.ui.common.R as UiCommonR

@Composable
internal fun ClickableComponent(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabledProvider: () -> Boolean = { true },
    descriptionProvider: (() -> String)? = null,
    icon: Painter? = null,
    endContent: @Composable (() -> Unit)? = null,
) {
    BaseTweakComponent(
        modifier = modifier,
        title = title,
        descriptionProvider = descriptionProvider,
        enabledProvider = enabledProvider,
        endContent = endContent,
        startContent =
            if (icon != null) {
                {
                    AdaptiveIcon(
                        painter = icon,
                        contentDescription = null,
                    )
                }
            } else {
                null
            },
        onClick = onClick,
    )
}

@Preview
@Composable
private fun ClickableComponentBasePreview() {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            Column {
                ClickableComponent(
                    title = "Clickable tweak with icon",
                    descriptionProvider = { "Clickable tweak summary" },
                    icon = painterResource(UiCommonR.drawable.happy_emphasized),
                    onClick = {},
                )
                ClickableComponent(
                    title = "Clickable tweak",
                    descriptionProvider = { "Clickable tweak summary" },
                    onClick = {},
                )
            }
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun ClickableComponentCompactLandscapePreview() {
    ClickableComponentBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun ClickableComponentMediumPortraitPreview() {
    ClickableComponentBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun ClickableComponentMediumLandscapePreview() {
    ClickableComponentBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun ClickableComponentExtendedPortraitPreview() {
    ClickableComponentBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun ClickableComponentExtendedLandscapePreview() {
    ClickableComponentBasePreview()
}
