package com.flixclusive.presentation.mobile.screens.preferences.settings.dialog_groups.subtitles.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.R
import com.flixclusive.domain.preferences.AppSettings
import com.flixclusive.domain.preferences.CaptionStylePreference.Companion.getTextStyle
import com.flixclusive.presentation.mobile.screens.preferences.settings.dialog_groups.subtitles.SubtitleSettingsDialog
import com.flixclusive.presentation.theme.subtitleBackgroundColors
import com.flixclusive.presentation.utils.ColorPickerUtils.AlphaBar

@Composable
fun SubtitleDialogFontBackgroundColor(
    appSettings: AppSettings,
    onChange: (Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    val (selectedOption, onOptionSelected) = remember {
        val currentColor = Color(appSettings.subtitleBackgroundColor)
            .copy(alpha = 1F)
            .toArgb()

        mutableIntStateOf(currentColor)
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
        title = stringResource(id = R.string.subtitles_background_color),
        onDismissRequest = onDismissRequest
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            subtitleBackgroundColors.forEach { (colorName, color) ->
                val colorInt = remember { color.toArgb() }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (colorInt == selectedOption),
                            onClick = {
                                onColorChange(colorInt)
                            }
                        ),
                    horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (colorInt == selectedOption),
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

            if(appSettings.subtitleBackgroundColor != 0) {
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

                            if(alpha == 0F)
                                onOptionSelected(0)

                            onChange(newColor.toArgb())
                        },
                    )
                }
            }
        }
    }
}