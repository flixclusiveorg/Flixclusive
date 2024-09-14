package com.flixclusive.feature.mobile.player.controls

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.player.util.PlayerUiUtil.rememberLocalPlayerManager
import com.flixclusive.feature.mobile.player.controls.common.EnlargedTouchableButton
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.ui.player.R as PlayerR
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun TopControls(
    modifier: Modifier = Modifier,
    currentEpisodeSelected: Episode?,
    onNavigationIconClick: () -> Unit,
    onPlayerSettingsClick: () -> Unit,
    onServersClick: () -> Unit,
) {
    val player by rememberLocalPlayerManager()

    val topFadeEdge = Brush.verticalGradient(0F to Color.Black, 0.9F to Color.Transparent)

    val titleStyle = MaterialTheme.typography.titleMedium.copy(
        fontSize = 14.sp
    )

    Box(
        modifier = modifier
            .drawBehind {
                drawRect(brush = topFadeEdge)
            }
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(top = 8.dp)
    ) {
        EnlargedTouchableButton(
            modifier = Modifier
                .align(Alignment.CenterStart),
            iconId = UiCommonR.drawable.left_arrow,
            contentDescription = stringResource(LocaleR.string.navigate_up),
            onClick = onNavigationIconClick
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.65F),
            contentAlignment = Alignment.Center
        ) {
            if (currentEpisodeSelected != null) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = titleStyle.toSpanStyle()) {
                            append("S${currentEpisodeSelected.season} E${currentEpisodeSelected.number}: ")
                        }
                        withStyle(
                            style = titleStyle.copy(
                                fontWeight = FontWeight.Light,
                                color = Color.White.onMediumEmphasis(emphasis = 0.8F),
                            ).toSpanStyle()
                        ) {
                            append(currentEpisodeSelected.title)
                        }
                    },
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            } else {
                Text(
                    text = player.displayTitle,
                    style = titleStyle,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Box(
            modifier = Modifier
                //.fillMaxWidth(0.17F) /*uncomment if adding cast feature already*/
                .width(100.dp)
                .align(Alignment.CenterEnd)
        ) {
            EnlargedTouchableButton(
                modifier = Modifier
                    //.align(Alignment.CenterStart), /*uncomment if adding cast feature already*/
                    .align(Alignment.CenterStart),
                iconId = PlayerR.drawable.round_cloud_queue_24,
                contentDescription = stringResource(id = LocaleR.string.servers),
                onClick = onServersClick
            )

            /*uncomment if adding cast feature already*/
            //TopControlsButton(
            //    modifier = Modifier
            //        .align(Alignment.Center),
            //    iconId = R.drawable.round_cast_24,
            //    contentDescription = stringResource(id = LocaleR.string.cast),
            //    onClick = onVideoSettingsClick
            //)

            EnlargedTouchableButton(
                modifier = Modifier
                    .align(Alignment.CenterEnd),
                iconId = UiCommonR.drawable.settings,
                contentDescription = stringResource(id = LocaleR.string.settings),
                onClick = onPlayerSettingsClick
            )
        }
    }
}

@Preview(
    device = "spec:parent=pixel_5,orientation=landscape",
    showSystemUi = true,
)
@Composable
private fun TopControlsPreview() {

    FlixclusiveTheme {
        Surface(
            color = MaterialTheme.colorScheme.primary
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, Color.Red),
            ) {
                TopControls(
                    modifier = Modifier
                        .border(1.dp, Color.Green)
                        .align(Alignment.TopCenter),
                    currentEpisodeSelected = Episode(
                        title = "Should've kissed you!",
                        number = 10,
                        season = 2
                    ),
                    onNavigationIconClick = {},
                    onPlayerSettingsClick = {},
                    onServersClick = {},
                )
            }
        }
    }
}