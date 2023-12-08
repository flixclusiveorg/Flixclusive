package com.flixclusive.presentation.mobile.screens.preferences.settings.dialog_groups.subtitles.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.R
import com.flixclusive.domain.preferences.AppSettings
import com.flixclusive.domain.preferences.AppSettings.Companion.CaptionStylePreference
import com.flixclusive.domain.preferences.AppSettings.Companion.CaptionStylePreference.Companion.getTextStyle
import com.flixclusive.presentation.mobile.screens.preferences.settings.dialog_groups.subtitles.SubtitleSettingsDialog

@Composable
fun SubtitleDialogFontStyle(
    appSettings: AppSettings,
    onChange: (CaptionStylePreference) -> Unit,
    onDismissRequest: () -> Unit
) {
    val (selectedOption, onOptionSelected) = remember { mutableStateOf(appSettings.subtitleFontStyle) }

    SubtitleSettingsDialog(
        appSettings = appSettings,
        title = stringResource(id = R.string.subtitles_font_style),
        onDismissRequest = onDismissRequest
    ) {
        Column {
            CaptionStylePreference.entries.forEach {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (it == selectedOption),
                            onClick = {
                                onOptionSelected(it)
                                onChange(it)
                            }
                        ),
                    horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (it == selectedOption),
                        onClick = {
                            onOptionSelected(it)
                            onChange(it)
                        }
                    )

                    Text(
                        text = it.toString(),
                        style = it.getTextStyle().copy(
                            fontSize = 18.sp
                        )
                    )
                }
            }
        }
    }
}