package com.flixclusive.feature.mobile.settings.component

import android.view.KeyEvent
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.drawables.R as UiCommonR

@Composable
internal fun SliderComponent(
    title: String,
    selectedValueProvider: () -> Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Float> = 0F..1F,
    steps: Int = 0,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    enabledProvider: () -> Boolean = { true },
    descriptionProvider: (() -> String)? = null,
    icon: Painter? = null,
) {
    BaseTweakComponent(
        modifier = modifier.onKeyEvent { keyEvent ->
            if (
                keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_UP
                && range.contains(selectedValueProvider())
            ) {
                val percentIncrement = range.endInclusive * 0.01f

                return@onKeyEvent when (keyEvent.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        val newValue = (selectedValueProvider() + percentIncrement)
                            .coerceIn(range)
                        onValueChange(newValue)
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        val newValue = (selectedValueProvider() - percentIncrement)
                            .coerceIn(range)
                        onValueChange(newValue)
                        true
                    }
                    else -> {
                        false
                    }
                }
            }

            false
        },
        title = title,
        descriptionProvider = descriptionProvider,
        enabledProvider = enabledProvider,
        extraContent = {
            Slider(
                enabled = enabledProvider(),
                value = selectedValueProvider(),
                steps = steps,
                valueRange = range,
                onValueChange = onValueChange,
                interactionSource = interactionSource,
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
            } else {
                null
            },
    )
}

@Preview
@Composable
private fun SliderComponentBasePreview() {
    var value by remember { mutableFloatStateOf(0F) }
    var value2 by remember { mutableFloatStateOf(0F) }

    FlixclusiveTheme {
        Surface(
            modifier =
                Modifier
                    .fillMaxSize(),
        ) {
            Column {
                SliderComponent(
                    title = "Slider tweak with icon",
                    descriptionProvider = { "Slider tweak summary" },
                    icon = painterResource(UiCommonR.drawable.happy_emphasized),
                    selectedValueProvider = { value },
                    onValueChange = { value = it },
                    range = 0F..100F,
                    steps = 10,
                )
                SliderComponent(
                    title = "Slider tweak",
                    descriptionProvider = { "Slider tweak summary" },
                    selectedValueProvider = { value2 },
                    onValueChange = { value2 = it },
                    range = 0F..1F,
                    steps = 100,
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
