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
fun PlayerDiskCacheSize(
    appSettings: AppSettings,
    onChange: (Long) -> Unit,
    onDismissRequest: () -> Unit
) {
    val sizesList = remember {
        listOf<Long>(
            0, 10, 20, 30, 40, 50, 60, 70, 80, 90,
            100, 150, 200, 250, 300, 350, 400, 450, 500
        )
    }
    val (selectedOption, onOptionSelected) = remember { mutableLongStateOf(appSettings.preferredDiskCacheSize) }

    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        val indexOfSelected = sizesList.indexOfFirst {
            it == appSettings.preferredDiskCacheSize
        }

        listState.animateScrollToItem(max(indexOfSelected, 0))
    }

    BaseSettingsDialog(
        title = stringResource(id = R.string.video_cache_size),
        onDismissRequest = onDismissRequest
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .heightIn(max = 400.dp)
        ) {
            items(sizesList) {
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
                        text = if(it == 0L) "None" else "${it}MB",
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