package com.flixclusive.presentation.mobile.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
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
import com.flixclusive.R
import com.flixclusive.presentation.mobile.theme.FlixclusiveMobileTheme

enum class PlayerSnackbarMessageType {
    Quality,
    Audio,
    Subtitle,
    PlaybackSpeed,
    Server,
    Source,
    Episode,
    Error;
}

data class PlayerSnackbarMessage(
    val message: String,
    val type: PlayerSnackbarMessageType,
    val duration: SnackbarDuration = SnackbarDuration.Short,
)

@Composable
fun PlayerSnackbar(
    modifier: Modifier = Modifier,
    messageData: PlayerSnackbarMessage,
    onDismissMessage: () -> Unit,
) {
    val message = messageData.message
    val type = messageData.type

    val snackbarShape = RoundedCornerShape(15)

    Box(
        modifier = modifier
            .padding(
                bottom = 15.dp,
                start = 15.dp
            )
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 450.dp)
                .height(50.dp)
                .background(
                    color = Color.Black.copy(0.50F),
                    shape = snackbarShape
                )
                .border(
                    width = 1.dp,
                    color = (
                            if (messageData.type == PlayerSnackbarMessageType.Error)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.primary)
                        .copy(0.5F),
                    shape = snackbarShape
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Normal,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .widthIn(max = 280.dp)
            )

            if (type != PlayerSnackbarMessageType.Episode) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                        .clickable {
                            onDismissMessage()
                        }
                ) {
                    Text(
                        text = stringResource(id = R.string.ok),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .padding(10.dp)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun PlayerSnackbarPreview() {
    FlixclusiveMobileTheme {
        Surface {
            PlayerSnackbar(
                messageData = PlayerSnackbarMessage(
                    message = "[Local] Attack.On.Titatasdas/as.da.sd.asd.asd.assrt",
                    type = PlayerSnackbarMessageType.Subtitle
                )
            ) {

            }
        }
    }
}