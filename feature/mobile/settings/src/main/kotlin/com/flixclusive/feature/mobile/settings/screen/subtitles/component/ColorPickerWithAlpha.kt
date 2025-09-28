package com.flixclusive.feature.mobile.settings.screen.subtitles.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.feature.mobile.settings.component.SliderComponent
import java.util.Locale
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun ColorPickerWithAlpha(
    title: String,
    selectedColor: Int,
    transparencyProvider: () -> Float,
    enabledProvider: () -> Boolean,
    colors: List<Color>,
    onAlphaChange: (Float) -> Unit,
    onPick: (Color) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    val currentColor = remember { mutableStateOf(Color(selectedColor).copy(alpha = 1F)) }
    val interactionSource = remember { MutableInteractionSource() }
    val isSliding by interactionSource.collectIsDraggedAsState()

    val transparencyUpdated by rememberUpdatedState(transparencyProvider)
    val onPickUpdated by rememberUpdatedState(onPick)

    LaunchedEffect(isSliding) {
        val isNotTheSameAlpha = currentColor.value.alpha != transparencyUpdated()
        if (!isSliding && isNotTheSameAlpha) {
            val newColorWithAlpha = currentColor.value.copy(transparencyUpdated())
            currentColor.value = newColorWithAlpha
            onPickUpdated(newColorWithAlpha)
        }
    }

    Column(modifier = modifier) {
        ColorPicker(
            title = title,
            description = description,
            selectedColor = currentColor.value.toArgb(),
            colors = colors,
            enabledProvider = enabledProvider,
            onPick = {
                onPickUpdated(it)
                onAlphaChange(1F)
                currentColor.value = it
            },
            modifier = Modifier
                .padding(top = getAdaptiveDp(10.dp))
        )

        SliderComponent(
            title = stringResource(LocaleR.string.bg_transparency),
            descriptionProvider = { String.format(Locale.getDefault(), "%.0f%%", transparencyProvider() * 100f) },
            selectedValueProvider = transparencyProvider,
            interactionSource = interactionSource,
            enabledProvider = enabledProvider,
            range = 0F..1F,
            steps = 0,
            onValueChange = onAlphaChange,
            modifier = Modifier.padding(vertical = getAdaptiveDp(10.dp)),
        )
    }
}

@Preview
@Composable
private fun ColorPickerWithAlphaBasePreview() {
    var selectedColor by remember { mutableIntStateOf(Color.White.toArgb()) }
    var alpha by remember { mutableFloatStateOf(0F) }

    FlixclusiveTheme {
        Surface {
            ColorPickerWithAlpha(
                selectedColor = selectedColor,
                transparencyProvider = { alpha },
                title = "Subtitle",
                enabledProvider = { true },
                colors =
                    listOf(
                        Color.White,
                        Color.Black,
                        Color.Red,
                        Color.Green,
                        Color.Blue,
                        Color.Yellow,
                        Color(0xFFFFA500),
                        Color(0xFF800080),
                        Color.Cyan,
                        Color.Gray,
                    ),
                onPick = { selectedColor = it.toArgb() },
                onAlphaChange = { newAlpha ->
                    alpha = newAlpha
                    selectedColor = Color(selectedColor).copy(alpha).toArgb()
                },
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun ColorPickerWithAlphaCompactLandscapePreview() {
    ColorPickerWithAlphaBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun ColorPickerWithAlphaMediumPortraitPreview() {
    ColorPickerWithAlphaBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun ColorPickerWithAlphaMediumLandscapePreview() {
    ColorPickerWithAlphaBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun ColorPickerWithAlphaExtendedPortraitPreview() {
    ColorPickerWithAlphaBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun ColorPickerWithAlphaExtendedLandscapePreview() {
    ColorPickerWithAlphaBasePreview()
}
