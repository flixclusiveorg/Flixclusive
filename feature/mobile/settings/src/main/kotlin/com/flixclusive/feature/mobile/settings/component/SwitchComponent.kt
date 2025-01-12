package com.flixclusive.feature.mobile.settings.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.core.ui.common.R as UiCommonR

@Composable
internal fun SwitchComponent(
    title: String,
    checked: () -> Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    descriptionProvider: (() -> String)? = null,
    enabledProvider: () -> Boolean = { true },
) {
    ClickableComponent(
        modifier = modifier,
        title = title,
        descriptionProvider = descriptionProvider,
        icon = icon,
        enabledProvider = enabledProvider,
        endContent = {
            Switch(
                checked = checked(),
                enabled = enabledProvider(),
                onCheckedChange = onCheckedChange,
                modifier = Modifier.padding(end = getAdaptiveDp(10.dp)),
            )
        },
        onClick = { onCheckedChange(!checked()) },
    )
}

@Preview
@Composable
private fun SwitchComponentBasePreview() {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            Column {
                SwitchComponent(
                    title = "Switch tweak with icon",
                    descriptionProvider = { "Switch tweak summary" },
                    icon = painterResource(UiCommonR.drawable.happy_emphasized),
                    checked = { true },
                    onCheckedChange = {},
                )
                SwitchComponent(
                    title = "Switch tweak",
                    descriptionProvider = { "Switch tweak summary" },
                    checked = { false },
                    onCheckedChange = {},
                )
                SwitchComponent(
                    title = "Switch tweak no summary",
                    checked = { false },
                    onCheckedChange = {},
                )
                SwitchComponent(
                    title = "Another switch tweak no summary",
                    checked = { false },
                    onCheckedChange = {},
                )
            }
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun SwitchComponentCompactLandscapePreview() {
    SwitchComponentBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun SwitchComponentMediumPortraitPreview() {
    SwitchComponentBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun SwitchComponentMediumLandscapePreview() {
    SwitchComponentBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun SwitchComponentExtendedPortraitPreview() {
    SwitchComponentBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun SwitchComponentExtendedLandscapePreview() {
    SwitchComponentBasePreview()
}
