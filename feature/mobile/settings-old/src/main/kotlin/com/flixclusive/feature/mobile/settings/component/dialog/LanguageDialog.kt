package com.flixclusive.feature.mobile.settings.component.dialog

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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.feature.mobile.settings.component.dialog.subtitles.SubtitleSettingsDialog
import com.flixclusive.model.datastore.AppSettings
import java.util.Locale
import kotlin.math.max

@Composable
internal fun LanguageDialog(
    appSettings: AppSettings,
    label: String,
    selectedOption: MutableState<String>,
    onChange: (Locale) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val listState = rememberLazyListState()

    val languages = remember {
        Locale.getAvailableLocales()
            .distinctBy { it.language }
    }

    LaunchedEffect(Unit) {
        val indexOfSelected = languages.indexOfFirst {
            it.language == selectedOption.value
        }

        listState.animateScrollToItem(max(indexOfSelected, 0))
    }

    SubtitleSettingsDialog(
        appSettings = appSettings,
        title = label,
        onDismissRequest = onDismissRequest,
        hidePreview = true,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .heightIn(max = 400.dp)
        ) {
            items(languages) {
                val isSelected = it.language == selectedOption.value
                    
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = isSelected,
                            onClick = {
                                selectedOption.value = it.language
                                onChange(it)
                            }
                        ),
                    horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = {
                            selectedOption.value = it.language
                            onChange(it)
                        }
                    )

                    Text(
                        text = "${it.displayLanguage} [${it.language}]",
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