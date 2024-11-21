package com.flixclusive.feature.mobile.settings.component.dialog.subtitles.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.subtitleColors
import com.flixclusive.core.ui.common.util.getTextStyle
import com.flixclusive.feature.mobile.settings.component.dialog.subtitles.SubtitleSettingsDialog
import com.flixclusive.model.datastore.AppSettings
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun SubtitleDialogFontColor(
    appSettings: AppSettings,
    onChange: (Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    val (selectedOption, onOptionSelected) = remember { mutableIntStateOf(appSettings.subtitleColor) }

    SubtitleSettingsDialog(
        appSettings = appSettings,
        title = stringResource(id = LocaleR.string.subtitles_color),
        onDismissRequest = onDismissRequest
    ) {
        Column {
            subtitleColors.forEach { (colorName, color) ->
                val colorInt = remember { color.toArgb() }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (colorInt == selectedOption),
                            onClick = {
                                onOptionSelected(colorInt)
                                onChange(colorInt)
                            }
                        ),
                    horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (colorInt == selectedOption),
                        onClick = {
                            onOptionSelected(colorInt)
                            onChange(colorInt)
                        }
                    )

                    Text(
                        text = colorName,
                        style = appSettings.subtitleFontStyle.getTextStyle().copy(
                            fontSize = 18.sp,
                            color = color
                        )
                    )
                }
            }
        }
    }
}