package com.flixclusive.presentation.mobile.screens.player.controls

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.flixclusive.R
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.presentation.common.player.utils.PlayerComposeUtils.rememberLocalPlayer
import com.flixclusive.presentation.mobile.screens.player.controls.common.EnlargedTouchableButton
import com.flixclusive.presentation.mobile.theme.FlixclusiveMobileTheme
import com.flixclusive.presentation.mobile.utils.ComposeMobileUtils.colorOnMediumEmphasisMobile

@Composable
fun TopControls(
    modifier: Modifier = Modifier,
    currentEpisodeSelected: TMDBEpisode?,
    onNavigationIconClick: () -> Unit,
    onPlayerSettingsClick: () -> Unit,
    onServersAndSourcesClick: () -> Unit,
) {
    val player = rememberLocalPlayer()

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
            iconId = R.drawable.left_arrow,
            contentDescription = "Back button",
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
                            append("S${currentEpisodeSelected.season} E${currentEpisodeSelected.episode}: ")
                        }
                        withStyle(
                            style = titleStyle.copy(
                                fontWeight = FontWeight.Light,
                                color = colorOnMediumEmphasisMobile(
                                    color = Color.White,
                                    emphasis = 0.8F,
                                ),
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
                .fillMaxWidth(0.12F)
                .align(Alignment.CenterEnd)
        ) {
            EnlargedTouchableButton(
                modifier = Modifier
                    //.align(Alignment.CenterStart), /*uncomment if adding cast feature already*/
                    .align(Alignment.CenterStart),
                iconId = R.drawable.round_cloud_queue_24,
                contentDescription = stringResource(id = R.string.server),
                onClick = onServersAndSourcesClick
            )

            /*uncomment if adding cast feature already*/
            //TopControlsButton(
            //    modifier = Modifier
            //        .align(Alignment.Center),
            //    iconId = R.drawable.round_cast_24,
            //    contentDescription = stringResource(id = R.string.cast),
            //    onClick = onVideoSettingsClick
            //)

            EnlargedTouchableButton(
                modifier = Modifier
                    .align(Alignment.CenterEnd),
                iconId = R.drawable.settings,
                contentDescription = stringResource(id = R.string.settings),
                onClick = onPlayerSettingsClick
            )
        }
    }
}

@Preview(
    device = "spec:parent=Realme 5,orientation=landscape",
    showSystemUi = true,
)
@Composable
private fun TopControlsPreview() {

    FlixclusiveMobileTheme {
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
                    currentEpisodeSelected = TMDBEpisode(
                        title = "Should've kissed you!",
                        episode = 10,
                        season = 2
                    ),
                    onNavigationIconClick = { /*TODO*/ },
                    onPlayerSettingsClick = {},
                    onServersAndSourcesClick = { /*TODO*/ },
                )
            }
        }
    }
}