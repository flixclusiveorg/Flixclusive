package com.flixclusive.feature.mobile.player.component.top

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.material3.PlainTooltipBox
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun PlayerTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    episode: Episode? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
    ) {
        PlainTooltipBox(
            description = stringResource(LocaleR.string.navigate_up)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(top = 5.dp, end = 5.dp)
                    .align(Alignment.CenterStart)
            ) {
                AdaptiveIcon(
                    painter = painterResource(UiCommonR.drawable.left_arrow),
                    contentDescription = stringResource(LocaleR.string.navigate_up)
                )
            }
        }

        PlayerLabel(
            title = title,
            episode = episode,
            modifier = Modifier
                .align(Alignment.Center)
        )
    }
}

@Composable
private fun PlayerLabel(
    title: String,
    episode: Episode?,
    modifier: Modifier = Modifier
) {
    val titleStyle = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp).asAdaptiveTextStyle()

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (episode != null) {
            Text(
                text = buildAnnotatedString {
                    withStyle(style = titleStyle.toSpanStyle()) {
                        append("S${episode.season} E${episode.number}: ")
                    }

                    withStyle(
                        style = titleStyle.copy(
                            fontWeight = FontWeight.Light,
                            color = Color.White.copy(alpha = 0.8F),
                        ).toSpanStyle()
                    ) {
                        append(episode.title)
                    }
                },
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        } else {
            Text(
                text = title,
                style = titleStyle,
            )
        }
    }

}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun PlayerTopBarBasePreview() {
    FlixclusiveTheme {
        Surface {
            PlayerTopBar(
                title = "Movie Title",
                episode = Episode(
                    id = "episodeId",
                    season = 1,
                    number = 1,
                    title = "Episode Title",
                ),
                onBack = {}
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun PlayerTopBarCompactLandscapePreview() {
    PlayerTopBarBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun PlayerTopBarMediumPortraitPreview() {
    PlayerTopBarBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun PlayerTopBarMediumLandscapePreview() {
    PlayerTopBarBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun PlayerTopBarExtendedPortraitPreview() {
    PlayerTopBarBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun PlayerTopBarExtendedLandscapePreview() {
    PlayerTopBarBasePreview()
}
