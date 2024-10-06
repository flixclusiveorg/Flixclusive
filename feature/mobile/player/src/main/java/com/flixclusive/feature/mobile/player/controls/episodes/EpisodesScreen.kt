package com.flixclusive.feature.mobile.player.controls.episodes

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.feature.mobile.player.controls.common.BasePopupScreen
import com.flixclusive.feature.mobile.player.controls.common.EnlargedTouchableButton
import com.flixclusive.feature.mobile.player.controls.episodes.composables.EpisodesRow
import com.flixclusive.feature.mobile.player.controls.episodes.composables.SeasonsRow
import com.flixclusive.model.database.WatchHistoryItem
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.film.common.tv.Season
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

@Composable
internal fun EpisodesScreen(
    modifier: Modifier = Modifier,
    seasonData: Resource<Season?>,
    availableSeasons: Int,
    currentEpisodeSelected: Episode,
    watchHistoryItem: WatchHistoryItem?,
    onSeasonChange: (Int) -> Unit,
    onEpisodeClick: (Episode) -> Unit,
    onClose: () -> Unit,
) {
    var selectedSeason by remember {
        mutableIntStateOf(seasonData.data?.number ?: currentEpisodeSelected.season)
    }

    BasePopupScreen(
        modifier = modifier,
        onDismiss = onClose
    ) {
        Row(
            modifier = Modifier
                .padding(start = 10.dp, top = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EnlargedTouchableButton(
                iconId = UiCommonR.drawable.round_close_24,
                contentDescription = stringResource(id = LocaleR.string.close_label),
                size = 45.dp,
                onClick = onClose
            )

            when (seasonData) {
                is Resource.Failure -> Unit
                Resource.Loading -> Unit
                is Resource.Success -> {
                    Text(
                        text = seasonData.data?.name ?: "Season ${seasonData.data?.number}",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        modifier = Modifier
                            .padding(start = 8.dp)
                    )
                }
            }
        }

        SeasonsRow(
            availableSeasons = availableSeasons,
            currentSeasonSelected = selectedSeason,
            onSeasonChange = {
                selectedSeason = it
                onSeasonChange(it)
            }
        )

        EpisodesRow(
            seasonData = seasonData,
            selectedSeason = selectedSeason,
            currentEpisodeSelected = currentEpisodeSelected,
            watchHistoryItem = watchHistoryItem,
            onSeasonChange = onSeasonChange,
            onEpisodeClick = onEpisodeClick,
            onClose = onClose
        )
    }
}

@Preview(
    device = "spec:parent=pixel_5,orientation=landscape",
    showSystemUi = true,
)
@Composable
private fun EpisodesScreenPreview() {
    val sampleEpisode = remember {
        Episode(
            number = 1,
            season = 1,
            title = "Sample Title",
            overview = "A car explodes in the middle of the city and kills a person. The cause of the explosion is unknown. Cha Jae Hwan, a detective from the Violent Crimes Unit, notices something strange at the scene. Eugene Hathaway, an FBI agent on duty in Korea, helps him take another look. Later, Jae Hwan chases after a suspect, and in the end, he gets faced with a dark figure.",
            image = "/eDXV3upl9iXqOtYaNuzAfZvmfTU.jpg"
        )
    }

    val seasonData = remember {
        Resource.Success(
            Season(
                number = 1,
                name = "Wano Arc",
                episodes = List(8) {
                    sampleEpisode
                },
            )
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
                    seasonData = seasonData,
                    availableSeasons = 8,
                    currentEpisodeSelected = Episode(),
                    watchHistoryItem = WatchHistoryItem(),
                    onSeasonChange = {},
                    onEpisodeClick = {},
                    onClose = {}
                )
            }
        }
    }
}