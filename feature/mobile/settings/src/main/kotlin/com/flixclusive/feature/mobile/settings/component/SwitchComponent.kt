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
    modifier: Modifier = Modifier,
    title: String,
    description: String? = null,
    icon: Painter? = null,
    enabled: Boolean = true,
    checked: Boolean = false,
    onCheckedChanged: (Boolean) -> Unit,
) {
    ClickableComponent(
        modifier = modifier,
        title = title,
        description = description,
        icon = icon,
        enabled = enabled,
        endContent = {
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChanged,
                modifier = Modifier.padding(end = getAdaptiveDp(10.dp)),
            )
        },
        onClick = { onCheckedChanged(!checked) },
    )
}

@Preview
@Composable
private fun SwitchComponentBasePreview() {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            Column {
                SwitchComponent(
                    title = "Switch tweak with icon",
                    description = "Switch tweak summary",
                    icon = painterResource(UiCommonR.drawable.happy_emphasized),
                    checked = true,
                    onCheckedChanged = {},
                )
                SwitchComponent(
                    title = "Switch tweak",
                    description = "Switch tweak summary",
                    checked = false,
                    onCheckedChanged = {},
                )
                SwitchComponent(
                    title = "Switch tweak no summary",
                    checked = false,
                    onCheckedChanged = {},
                )
                SwitchComponent(
                    title = "Another switch tweak no summary",
                    checked = false,
                    onCheckedChanged = {},
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