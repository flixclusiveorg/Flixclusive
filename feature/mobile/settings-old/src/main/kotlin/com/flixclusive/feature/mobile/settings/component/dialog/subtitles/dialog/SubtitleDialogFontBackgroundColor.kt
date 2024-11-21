package com.flixclusive.feature.mobile.settings.component.dialog.subtitles.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.theme.subtitleBackgroundColors
import com.flixclusive.core.ui.common.util.getTextStyle
import com.flixclusive.feature.mobile.settings.component.dialog.subtitles.SubtitleSettingsDialog
import com.flixclusive.feature.mobile.settings.util.ColorPickerHelper.AlphaBar
import com.flixclusive.model.datastore.AppSettings
import kotlin.math.abs
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun SubtitleDialogFontBackgroundColor(
    appSettings: AppSettings,
    onChange: (Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    val (selectedOption, onOptionSelected) = remember {
        mutableIntStateOf(0)
    }

    LaunchedEffect(Unit) {
        val preferredColor = appSettings.subtitleBackgroundColor
        val color = Color(preferredColor)
            .copy(alpha = abs(preferredColor).toFloat().coerceIn(0F, 1F))
            .toArgb()

        onOptionSelected(color)
    }

    val (alpha, onAlphaChange) = remember {
        val currentColor = Color(appSettings.subtitleBackgroundColor)
        mutableFloatStateOf(currentColor.alpha)
    }

    val onColorChange = { color: Int ->
        val isComingFromTransparent = color != 0 && alpha == 0F

        if(isComingFromTransparent)
            onAlphaChange(1F)

        onOptionSelected(color)
        onChange(color)
    }

    SubtitleSettingsDialog(
        appSettings = appSettings,
        title = stringResource(id = LocaleR.string.subtitles_background_color),
        onDismissRequest = onDismissRequest
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            subtitleBackgroundColors.forEach { (colorName, color) ->
                val colorInt = remember { color.toArgb() }
                val isSelected = remember(selectedOption) { colorInt == selectedOption }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = isSelected,
                            onClick = {
                                onColorChange(colorInt)
                            }
                        ),
                    horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = {
                            onColorChange(colorInt)
                        }
                    )

                    Text(
                        text = colorName,
                        style = appSettings.subtitleFontStyle.getTextStyle().copy(
                            fontSize = 18.sp,
                            color = contentColorFor(color),
                            background = color
                        )
                    )
                }
            }

            if(selectedOption != 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(45.dp)
                        .padding(bottom = 15.dp)
                ) {
                    AlphaBar(
                        color = {
                            Color(appSettings.subtitleBackgroundColor)
                                .copy(alpha = alpha)
                        },
                        onAlphaChanged = { alpha, newColor ->
                            onAlphaChange(alpha)

                            if(alpha == 0F) {
                                onOptionSelected(0)
                                onChange(0)
                                return@AlphaBar
                            }

                            onChange(newColor.toArgb())
                        },
                    )
                }
            }
        }
    }
}

@Preview
@Composable
internal fun SubtitleDialogFontBackgroundColorPreview() {
    val (appSettings, onAppSettingsChange) = remember {
        mutableStateOf(
            AppSettings(
                subtitleBackgroundColor = Color.Red.copy(alpha = 0F).toArgb()
            )
        )
    }

    FlixclusiveTheme {
        Surface {
            SubtitleDialogFontBackgroundColor(
                appSettings = appSettings,
                onChange = {
                    onAppSettingsChange(appSettings.copy(subtitleBackgroundColor = it))
                },
                onDismissRequest = {}
            )
        }
    }
}