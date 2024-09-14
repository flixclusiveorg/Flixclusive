package com.flixclusive.feature.mobile.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.player.PlayerSnackbarMessage
import com.flixclusive.core.ui.player.PlayerSnackbarMessageType
import com.flixclusive.core.locale.UiText
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun PlayerSnackbar(
    modifier: Modifier = Modifier,
    messageData: PlayerSnackbarMessage,
    onDismissMessage: () -> Unit,
) {
    val message = messageData.message
    val type = messageData.type

    val snackbarShape = RoundedCornerShape(15)
    val borderColor = when (messageData.type) {
        PlayerSnackbarMessageType.Error -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = modifier
            .padding(
                bottom = 15.dp,
                start = 15.dp
            )
    ) {
        Row(
            modifier = Modifier
                .heightIn(50.dp)
                .background(
                    color = Color.Black.copy(0.50F),
                    shape = snackbarShape
                )
                .border(
                    width = 1.dp,
                    color = borderColor.copy(0.5F),
                    shape = snackbarShape
                )
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Text(
                text = message.asString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Normal,
                color = Color.White,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(weight = 0.9F, fill = false)
                    .padding(vertical = 8.dp)
            )

            if (type != PlayerSnackbarMessageType.Episode) {
                Text(
                    text = stringResource(id = LocaleR.string.ok),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .clickable {
                            onDismissMessage()
                        }
                )
            }
        }
    }
}

@Preview
@Composable
private fun PlayerSnackbarPreview() {
    FlixclusiveTheme {
        Surface {
            PlayerSnackbar(
                messageData = PlayerSnackbarMessage(
                    message = UiText.StringValue("[Local] Attack.On.Titatasda.srt"),
                    type = PlayerSnackbarMessageType.Subtitle
                )
            ) {

            }
        }
    }
}