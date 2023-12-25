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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.R
import com.flixclusive.domain.preferences.AppSettings
import com.flixclusive.domain.preferences.AppSettings.Companion.possibleAvailableSeekIncrementMs
import com.flixclusive.presentation.mobile.screens.preferences.settings.dialog_groups.BaseSettingsDialog

@Composable
fun PlayerSeekLength(
    appSettings: AppSettings,
    onChange: (Long) -> Unit,
    onDismissRequest: () -> Unit
) {
    val (selectedOption, onOptionSelected) = remember { mutableLongStateOf(appSettings.preferredSeekAmount) }

    BaseSettingsDialog(
        title = stringResource(id = R.string.seek_length_label),
        onDismissRequest = onDismissRequest
    ) {
        Column {
            possibleAvailableSeekIncrementMs.forEach {
                val inMs = it * 1000
                val isSelected = inMs == selectedOption

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = isSelected,
                            onClick = {
                                onOptionSelected(inMs)
                                onChange(inMs)
                            }
                        ),
                    horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = {
                            onOptionSelected(inMs)
                            onChange(inMs)
                        }
                    )

                    Text(
                        text = "$it seconds",
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