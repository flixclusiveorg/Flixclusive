package com.flixclusive.feature.mobile.player.component.episodes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.WatchStatus
import com.flixclusive.core.presentation.common.extensions.noOpClickable
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.domain.provider.model.EpisodeWithProgress
import com.flixclusive.domain.provider.model.SeasonWithProgress
import com.flixclusive.feature.mobile.player.component.episodes.component.EpisodesRow
import com.flixclusive.feature.mobile.player.component.episodes.component.SeasonsRow
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.film.common.tv.Season
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun EpisodesScreen(
    modifier: Modifier = Modifier,
    seasons: List<Season>,
    currentSeason: SeasonWithProgress,
    currentEpisode: Episode,
    onSeasonChange: (Season) -> Unit,
    onEpisodeClick: (Episode) -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler {
        onDismiss()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.9F))
    ) {
        // Block touches
        Box(
            modifier = Modifier
            .fillMaxSize()
            .noOpClickable()
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.TopStart),
        ) {
            Row(
                modifier = Modifier
                    .padding(start = 10.dp, top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentSeason.title,
                    style = MaterialTheme.typography.headlineSmall
                        .asAdaptiveTextStyle(size = 22.sp)
                        .copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .weight(1f)
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(end = 20.dp)
                ) {
                    AdaptiveIcon(
                        painter = painterResource(id = UiCommonR.drawable.round_close_24),
                        contentDescription = stringResource(id = LocaleR.string.close),
                        tint = Color.White
                    )
                }
            }

            SeasonsRow(
                seasons = seasons,
                currentSeason = currentSeason.season,
                onSeasonChange = {
                    onSeasonChange(it)
                },
                modifier = Modifier.padding(bottom = 10.dp)
            )

            EpisodesRow(
                seasonData = currentSeason,
                currentEpisodeSelected = currentEpisode,
                onEpisodeClick = onEpisodeClick,
                onClose = onDismiss
            )
        }
    }
}

@Preview(
    device = "spec:parent=pixel_5,orientation=landscape",
    showSystemUi = true,
)
@Composable
private fun EpisodesScreenPreview() {
    val sampleShow = remember { DummyDataForPreview.getTvShow() }
    val sampleEpisode = remember {
        val season = sampleShow.seasons.first()
        val episode = season.episodes.first()
        EpisodeWithProgress(
            episode = episode,
            watchProgress = EpisodeProgress(
                episodeNumber = episode.number,
                progress = 1200L,
                duration = 2400L,
                filmId = sampleShow.identifier,
                ownerId = 1,
                status = WatchStatus.WATCHING,
                seasonNumber = season.number,
            )
        )
    }

    val seasonData = remember {
        val season = sampleShow.seasons.first()
        SeasonWithProgress(
            season = season,
            episodes = List(20) {
                sampleEpisode.copy(
                    episode = sampleEpisode.episode.copy(
                        number = it + 1,
                        title = "Episode ${it + 1}"
                    )
                )
            },
        )
    }

    FlixclusiveTheme {
        Surface {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Image(
                    painter = painterResource(id = UiCommonR.drawable.sample_movie_subtitle_preview),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                )

                EpisodesScreen(
                    currentSeason = seasonData,
                    seasons = sampleShow.seasons,
                    currentEpisode = sampleEpisode.episode,
                    onSeasonChange = {},
                    onEpisodeClick = {},
                    onDismiss = {}
                )
            }
        }
    }
}
