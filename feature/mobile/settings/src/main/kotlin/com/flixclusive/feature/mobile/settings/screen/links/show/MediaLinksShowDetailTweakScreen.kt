package com.flixclusive.feature.mobile.settings.screen.links.show

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.domain.Async.Companion.AsyncAnimatedContent
import com.flixclusive.core.database.entity.provider.EpisodeLinks
import com.flixclusive.core.database.entity.provider.SeasonLinks
import com.flixclusive.core.navigation.navigator.NavigateBack
import com.flixclusive.core.navigation.navigator.NavigateToManageMediaLinksScreen
import com.flixclusive.core.presentation.mobile.components.EmptyDataMessage
import com.flixclusive.core.presentation.mobile.components.Placeholder
import com.flixclusive.core.presentation.mobile.components.material3.topbar.ActionButton
import com.flixclusive.core.presentation.mobile.components.material3.topbar.CommonTopBar
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.LocalGlobalScaffoldPadding
import com.flixclusive.feature.mobile.settings.screen.links.manage.ManageMediaLinksTweakScreenArgs
import com.flixclusive.feature.mobile.settings.screen.links.util.CacheLinksFormatUtil
import com.flixclusive.feature.mobile.settings.screen.links.util.LinkUtil.toRelativeTime
import com.flixclusive.model.media.common.tv.Episode
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

interface NavigatorMediaLinksShowDetailTweakScreen :
    NavigateBack,
    NavigateToManageMediaLinksScreen

@Destination<ExternalModuleGraph>(
    navArgs = ManageMediaLinksTweakScreenArgs::class
)
@Composable
internal fun MediaLinksShowDetailTweakScreen(
    args: ManageMediaLinksTweakScreenArgs,
    navigator: NavigatorMediaLinksShowDetailTweakScreen,
    viewModel: MediaLinksShowDetailTweakViewModel = hiltViewModel()
) {
    val episodes by viewModel.episodes.collectAsStateWithLifecycle()
    val selectedSeason by viewModel.selectedSeason.collectAsStateWithLifecycle()
    val availableSeasons by viewModel.availableSeasons.collectAsStateWithLifecycle()

    MediaLinksShowDetailTweakScreenContent(
        title = args.media.title,
        episodes = episodes,
        selectedSeason = selectedSeason,
        availableSeasons = availableSeasons,
        onSeasonChange = viewModel::onSeasonChange,
        onDeleteSeason = { selectedSeason?.let { viewModel.onDeleteSeason(it) } },
        onNavigateBack = navigator::navigateBack,
        onEpisodeClick = { episodeNumber ->
            navigator.navigateToManageMediaLinksScreen(
                media = viewModel.show,
                episode = Episode(
                    id = "${viewModel.show.id}-$selectedSeason-$episodeNumber",
                    number = episodeNumber,
                    season = selectedSeason ?: 1,
                    title = "Episode $episodeNumber",
                    isReleased = true
                )
            )
        }
    )
}

@Composable
internal fun MediaLinksShowDetailTweakScreenContent(
    title: String,
    episodes: Async<List<EpisodeLinks>>,
    selectedSeason: Int?,
    availableSeasons: List<SeasonLinks>,
    onSeasonChange: (Int) -> Unit,
    onDeleteSeason: () -> Unit,
    onNavigateBack: () -> Unit,
    onEpisodeClick: (Int) -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets(),
        topBar = {
            CommonTopBar(
                title = { Text(title) },
                navigationIcon = {
                    ActionButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(UiCommonR.drawable.left_arrow),
                            contentDescription = null
                        )
                    }
                }
            )
        },
        modifier = Modifier.padding(LocalGlobalScaffoldPadding.current)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableSeasons) { season ->
                    FilterChip(
                        selected = selectedSeason == season.number,
                        onClick = { onSeasonChange(season.number) },
                        label = { Text("S${season.number}") },
                        shape = MaterialTheme.shapes.small,
                        border = null,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.Transparent,
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            HorizontalDivider()

            AsyncAnimatedContent(
                targetState = episodes,
                loadingContent = {
                    Column(modifier = Modifier.padding(16.dp)) {
                        repeat(5) {
                            Placeholder(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .padding(vertical = 8.dp)
                            )
                        }
                    }
                },
                errorContent = {}
            ) { state ->
                val data = state()
                if (data.isEmpty()) {
                    EmptyDataMessage(
                        modifier = Modifier.fillMaxSize(),
                        emojiHeader = "🫥",
                        title = stringResource(LocaleR.string.label_no_cached_media)
                    )
                } else if (selectedSeason != null) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${stringResource(
                                        LocaleR.string.label_format_untitled_season,
                                        selectedSeason
                                    )} " +
                                        "• ${data.size} ${stringResource(LocaleR.string.episodes)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                TextButton(onClick = onDeleteSeason) {
                                    Text(
                                        text = stringResource(LocaleR.string.delete),
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }

                        items(data) { episodeLinks ->
                            EpisodeLinkRow(
                                episodeNumber = episodeLinks.number,
                                seasonNumber = selectedSeason,
                                count = episodeLinks.count,
                                lastUpdated = episodeLinks.lastUpdated,
                                onClick = { onEpisodeClick(episodeLinks.number) }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeLinkRow(
    episodeNumber: Int,
    seasonNumber: Int,
    count: Int,
    lastUpdated: Long?,
    onClick: () -> Unit
) {
    val resources = LocalResources.current
    val relativeTime = remember(lastUpdated) {
        lastUpdated?.let { java.util.Date(it).toRelativeTime(resources) } ?: ""
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = CacheLinksFormatUtil.getFormattedTitle(seasonNumber, episodeNumber),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "$count links",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (relativeTime.isNotEmpty()) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = relativeTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Icon(
            painter = painterResource(UiCommonR.drawable.arrow_right_thin),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview
@Composable
private fun MediaLinksShowDetailTweakScreenContentPreview() {
    FlixclusiveTheme {
        Surface {
            MediaLinksShowDetailTweakScreenContent(
                title = "Sample Show",
                episodes = Async.Success(
                    List(5) {
                        EpisodeLinks(
                            number = it + 1,
                            count = 10,
                            lastUpdated = System.currentTimeMillis() - (it * 3600000L)
                        )
                    }
                ),
                selectedSeason = 1,
                availableSeasons = List(3) { SeasonLinks(it + 1, 10) },
                onSeasonChange = {},
                onDeleteSeason = {},
                onNavigateBack = {},
                onEpisodeClick = {}
            )
        }
    }
}
