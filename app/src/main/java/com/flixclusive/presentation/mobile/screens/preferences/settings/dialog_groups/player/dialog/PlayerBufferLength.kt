package com.flixclusive.presentation.mobile.screens.preferences.settings.dialog_groups.player.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.flixclusive.presentation.mobile.screens.preferences.settings.dialog_groups.BaseSettingsDialog
import kotlin.math.max

@Composable
fun PlayerBufferLength(
    appSettings: AppSettings,
    onChange: (Long) -> Unit,
    onDismissRequest: () -> Unit
) {
    val sizesList = remember {
        mapOf(
            50L to "50s (Default)",
            60L to "1min",
            90L to "1min 30s",
            120L to "2min",
            150L to "2min 30s",
            180L to "3min",
            210L to "3min 30s",
            240L to "4min",
            300L to "5min",
            360L to "6min",
            420L to "7min",
            480L to "8min",
            540L to "9min",
            600L to "10min",
            900L to "15min",
            1200L to "20min",
            1800L to "30min"
        )
    }
    val (selectedOption, onOptionSelected) = remember { mutableLongStateOf(appSettings.preferredVideoBufferMs) }

    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        val indexOfSelected = sizesList.keys.indexOfFirst {
            it == appSettings.preferredVideoBufferMs
        }

        listState.animateScrollToItem(max(indexOfSelected, 0))
    }

    BaseSettingsDialog(
        title = stringResource(id = R.string.video_buffer_max_length),
        onDismissRequest = onDismissRequest
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .heightIn(max = 400.dp)
        ) {
            items(sizesList.keys.toList()) {
                val isSelected = it == selectedOption

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = isSelected,
                            onClick = {
                                onOptionSelected(it)
                                onChange(it)
                            }
                        ),
                    horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = {
                            onOptionSelected(it)
                            onChange(it)
                        }
                    )

                    Text(
                        text = sizesList[it] ?: "",
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