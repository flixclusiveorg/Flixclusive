package com.flixclusive.feature.mobile.settings.component.dialog.player.dialog

import android.text.format.Formatter.formatShortFileSize
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.util.common.ui.UiText
import com.flixclusive.feature.mobile.settings.component.dialog.BaseSettingsDialog
import com.flixclusive.model.datastore.AppSettings
import com.flixclusive.model.datastore.DEFAULT_PLAYER_CACHE_SIZE_AMOUNT
import kotlin.math.max
import com.flixclusive.core.util.R as UtilR

@Composable
internal fun PlayerDiskCacheSize(
    appSettings: AppSettings,
    onChange: (Long) -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val sizesList = remember {
        listOf<Long>(
            0, 10, 20, 30, 40, 50, 60, 70, 80, 90,
            100, 150, 200, 250, 300, 350, 400, 450, 500,
            1000, 2000, -1
        )
    }
    val (selectedOption, onOptionSelected) = remember { mutableLongStateOf(appSettings.preferredDiskCacheSize) }

    val listState = rememberLazyListState()

    fun onChangeValue(value: Long) {
        onOptionSelected(value)
        onChange(value)
    }

    LaunchedEffect(Unit) {
        val indexOfSelected = sizesList.indexOfFirst {
            it == appSettings.preferredDiskCacheSize
        }

        listState.animateScrollToItem(max(indexOfSelected, 0))
    }

    BaseSettingsDialog(
        title = stringResource(id = UtilR.string.video_cache_size),
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
                                onChangeValue(it)
                            }
                        ),
                    horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = {
                            onChangeValue(it)
                        }
                    )

                    val sizeName = remember {
                        when (it) {
                            0L -> UiText.StringResource(UtilR.string.none_label).asString(context)
                            -1L -> UiText.StringResource(UtilR.string.no_cache_limit_label).asString(context)
                            else -> formatShortFileSize(
                                /* context = */ context,
                                /* sizeBytes = */ it * 1000L * 1000L
                            ) + if(it == DEFAULT_PLAYER_CACHE_SIZE_AMOUNT) " " + UiText.StringResource(UtilR.string.default_label).asString(context) else ""
                        }
                    }

                    Text(
                        text = sizeName,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 16.sp,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                        )
                    )
                }
            }
        }
    }
}