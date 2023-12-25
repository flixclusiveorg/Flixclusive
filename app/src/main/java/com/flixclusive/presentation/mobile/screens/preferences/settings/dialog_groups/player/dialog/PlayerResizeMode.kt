package com.flixclusive.presentation.mobile.screens.preferences.settings.dialog_groups.player.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.R
import com.flixclusive.domain.preferences.AppSettings
import com.flixclusive.domain.preferences.AppSettings.Companion.resizeModes
import com.flixclusive.presentation.mobile.screens.preferences.settings.dialog_groups.BaseSettingsDialog

@Composable
fun PlayerResizeMode(
    appSettings: AppSettings,
    onChange: (Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    val (selectedOption, onOptionSelected) = remember { mutableStateOf(appSettings.preferredResizeMode) }

    BaseSettingsDialog(
        title = stringResource(id = R.string.resize_mode),
        onDismissRequest = onDismissRequest
    ) {
        Column {
            resizeModes.forEach { (key, value) ->
                val isSelected = value == selectedOption

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = isSelected,
                            onClick = {
                                onOptionSelected(value)
                                onChange(value)
                            }
                        ),
                    horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = {
                            onOptionSelected(value)
                            onChange(value)
                        }
                    )

                    Text(
                        text = key,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 16.sp,
                            fontWeight = if(isSelected) FontWeight.Medium else FontWeight.Normal
                        )
                    )
                }
            }
        }
    }
}