package com.flixclusive.feature.mobile.player.controls.dialogs.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.applyDropShadow
import com.flixclusive.core.ui.common.util.createTextFieldValue
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.player.util.PlayerUiUtil.rememberLocalPlayerManager
import com.flixclusive.core.ui.player.R as PlayerR
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun SubtitleSyncPanel(
    modifier: Modifier = Modifier
) {
    val player by rememberLocalPlayerManager()

    var textFieldValue by remember(player.subtitleOffset) {
        mutableStateOf(player.subtitleOffset.toString().createTextFieldValue())
    }

    fun changeOffset(by: Long) {
        textFieldValue = by.toString().createTextFieldValue()
        player.onSubtitleOffsetChange(by)
    }

    val mediumEmphasis = LocalContentColor.current.onMediumEmphasis()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(LocaleR.string.sync_subtitles),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                color = LocalContentColor.current.onMediumEmphasis(emphasis = 0.8F),
                textAlign = TextAlign.Center
            ),
            modifier = Modifier
                .padding(bottom = 5.dp)
        )

        TextField(
            modifier = Modifier
                .fillMaxWidth(0.8F),
            value = textFieldValue,
            singleLine = true,
            textStyle = MaterialTheme.typography.labelLarge.applyDropShadow().copy(
                textAlign = TextAlign.Center,
            ),
            onValueChange = {
                if (it.text.isBlank()) {
                    player.onSubtitleOffsetChange(0)
                    textFieldValue = "0".createTextFieldValue()
                }

                it.text.toLongOrNull()?.let { ms ->
                    player.onSubtitleOffsetChange(ms)
                    textFieldValue = ms.toString().createTextFieldValue()
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            OffsetButton(
                drawableId = PlayerR.drawable.round_keyboard_double_arrow_left_24,
                contentDescription = stringResource(LocaleR.string.subtract_1000ms_content_description),
                changeOffset = {
                    changeOffset(player.subtitleOffset - 1000)
                }
            )

            OffsetButton(
                drawableId = PlayerR.drawable.chevron_left_black_24dp,
                contentDescription = stringResource(LocaleR.string.subtract_500ms_content_description),
                changeOffset = {
                    changeOffset(player.subtitleOffset - 500)
                }
            )

            OffsetButton(
                drawableId = PlayerR.drawable.chevron_right_black_24dp,
                contentDescription = stringResource(LocaleR.string.add_500ms_content_description),
                changeOffset = {
                    changeOffset(player.subtitleOffset + 500)
                }
            )

            OffsetButton(
                drawableId = PlayerR.drawable.round_keyboard_double_arrow_right_24,
                contentDescription = stringResource(LocaleR.string.add_1000ms_content_description),
                changeOffset = {
                    changeOffset(player.subtitleOffset + 1000)
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 5.dp)
                .padding(horizontal = 10.dp)
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth(),
                text = stringResource(id = LocaleR.string.add_sync_offset_message),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = mediumEmphasis,
                    textAlign = TextAlign.Center
                )
            )

            Text(
                modifier = Modifier
                    .fillMaxWidth(),
                text = stringResource(id = LocaleR.string.subtract_sync_offset_message),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = mediumEmphasis,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

@Composable
private fun OffsetButton(
    @DrawableRes drawableId: Int,
    contentDescription: String?,
    changeOffset: () -> Unit,
) {
    IconButton(
        onClick = {
            changeOffset()
        },
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier
            .border(1.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
    ) {
        Icon(
            painter = painterResource(drawableId),
            contentDescription = contentDescription
        )
    }
}

@Preview
@Composable
private fun SubtitleSyncPanelPreview() {
//    val (offset, onOffsetChange) = remember {
//        mutableLongStateOf(0L)
//    }

    FlixclusiveTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.onSurface)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(250.dp)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.6F),
                        RoundedCornerShape(10.dp)
                    )
            ) {
                SubtitleSyncPanel()
            }
        }
    }
}