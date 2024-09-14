package com.flixclusive.feature.mobile.settings.component.dialog.subtitles.dialog

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.feature.mobile.settings.component.dialog.subtitles.BorderedText
import com.flixclusive.core.ui.common.util.getTextStyle
import com.flixclusive.feature.mobile.settings.component.dialog.subtitles.SubtitleSettingsDialog
import com.flixclusive.model.datastore.AppSettings
import com.flixclusive.model.datastore.player.CaptionEdgeTypePreference
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun SubtitleDialogEdgeType(
    appSettings: AppSettings,
    onChange: (CaptionEdgeTypePreference) -> Unit,
    onDismissRequest: () -> Unit
) {
    val (selectedOption, onOptionSelected) = remember { mutableStateOf(appSettings.subtitleEdgeType) }

    SubtitleSettingsDialog(
        appSettings = appSettings,
        title = stringResource(id = LocaleR.string.subtitles_edge_type),
        onDismissRequest = onDismissRequest
    ) {
        Column {
            CaptionEdgeTypePreference.entries.forEach {
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

                    when (it) {
                        CaptionEdgeTypePreference.Drop_Shadow -> {
                            Text(
                                text = it.toString().replace("_", " "),
                                style = appSettings.subtitleFontStyle.getTextStyle().copy(
                                    fontSize = 16.sp,
                                    shadow = Shadow(
                                        offset = Offset(6F, 6F),
                                        blurRadius = 3f,
                                        color = Color(appSettings.subtitleEdgeType.color),
                                    ),
                                )
                            )
                        }
                        CaptionEdgeTypePreference.Outline -> {
                            BorderedText(
                                text = it.toString(),
                                borderColor = Color(appSettings.subtitleEdgeType.color),
                                style = appSettings.subtitleFontStyle.getTextStyle().copy(
                                    fontSize = 16.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}