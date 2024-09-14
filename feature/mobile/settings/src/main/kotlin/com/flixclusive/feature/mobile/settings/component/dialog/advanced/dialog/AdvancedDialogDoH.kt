package com.flixclusive.feature.mobile.settings.component.dialog.advanced.dialog

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.feature.mobile.settings.component.dialog.BaseSettingsDialog
import com.flixclusive.model.datastore.AppSettings
import com.flixclusive.model.datastore.network.DoHPreference
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun AdvancedDialogDoH(
    appSettings: AppSettings,
    onChange: (DoHPreference) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val (selectedOption, onOptionSelected) = remember { mutableStateOf(appSettings.dns) }

    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        var indexOfSelected = DoHPreference.entries.toTypedArray().indexOfFirst {
            it == appSettings.dns
        }

        if(indexOfSelected == -1)
            indexOfSelected = 0

        listState.animateScrollToItem(indexOfSelected)
    }

    BaseSettingsDialog(
        title = stringResource(id = LocaleR.string.doh),
        onDismissRequest = onDismissRequest,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .heightIn(max = 400.dp)
        ) {
            items(DoHPreference.entries.toTypedArray()) {
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
                        text = it.name,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            fontWeight = if(isSelected) FontWeight.Medium else FontWeight.Normal
                        )
                    )
                }
            }
        }
    }
}