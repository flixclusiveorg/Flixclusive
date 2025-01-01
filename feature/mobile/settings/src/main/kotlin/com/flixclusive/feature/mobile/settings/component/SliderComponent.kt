package com.flixclusive.feature.mobile.settings.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.adaptive.AdaptiveIcon
import com.flixclusive.core.ui.common.R as UiCommonR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SliderComponent(
    selectedValue: Float,
    title: String,
    enabled: Boolean = true,
    range: ClosedFloatingPointRange<Float> = 0F..1F,
    description: String? = null,
    icon: Painter? = null,
    modifier: Modifier = Modifier,
    onValueChange: (Float) -> Unit
) {
    BaseTweakComponent(
        modifier = modifier,
        title = title,
        description = description,
        enabled = enabled,
        extraContent = {
            Slider(
                enabled = enabled,
                value = selectedValue,
                valueRange = range,
                onValueChange = onValueChange
            )
        },
        startContent =
            if (icon != null) {
                {
                    AdaptiveIcon(
                        painter = icon,
                        contentDescription = null,
                    )
                }
            } else null,
    )
}

@Preview
@Composable
private fun SliderComponentBasePreview() {
    var value by remember { mutableFloatStateOf(0.5F) }
    var value2 by remember { mutableFloatStateOf(0F) }

    FlixclusiveTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column {
                SliderComponent(
                    title = "Slider tweak with icon",
                    description = "Slider tweak summary",
                    icon = painterResource(UiCommonR.drawable.happy_emphasized),
                    selectedValue = value,
                    onValueChange = { value = it},
                )
                SliderComponent(
                    title = "Slider tweak",
                    description = "Slider tweak summary",
                    selectedValue = value2,
                    onValueChange = { value2 = it},
                )
            }
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun SliderComponentCompactLandscapePreview() {
    SliderComponentBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun SliderComponentMediumPortraitPreview() {
    SliderComponentBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun SliderComponentMediumLandscapePreview() {
    SliderComponentBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun SliderComponentExtendedPortraitPreview() {
    SliderComponentBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun SliderComponentExtendedLandscapePreview() {
    SliderComponentBasePreview()
}