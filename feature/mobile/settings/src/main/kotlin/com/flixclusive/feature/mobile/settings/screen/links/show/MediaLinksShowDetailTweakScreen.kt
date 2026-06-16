package com.flixclusive.feature.mobile.settings.screen.links.show

//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.PaddingValues
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.LazyRow
//import androidx.compose.foundation.lazy.items
//import androidx.compose.material3.CircularProgressIndicator
//import androidx.compose.material3.FilterChip
//import androidx.compose.material3.HorizontalDivider
//import androidx.compose.material3.Icon
//import androidx.compose.material3.ListItem
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Text
//import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
//import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.ReadOnlyComposable
//import androidx.compose.runtime.derivedStateOf
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.rememberCoroutineScope
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.res.stringResource
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.util.fastDistinctBy
//import androidx.compose.ui.util.fastFilter
//import androidx.compose.ui.util.fastFirstOrNull
//import androidx.compose.ui.util.fastMapNotNull
//import androidx.datastore.preferences.core.stringPreferencesKey
//import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
//import androidx.lifecycle.compose.collectAsStateWithLifecycle
//import com.flixclusive.core.common.domain.Async.Companion.AsyncAnimatedContent
//import com.flixclusive.core.datastore.model.FlixclusivePrefs
//import com.flixclusive.core.presentation.common.components.MediaCover
//import com.flixclusive.core.presentation.mobile.components.EmptyDataMessage
//import com.flixclusive.core.presentation.mobile.util.MobileUiUtil.getAdaptiveMediaCardWidth
//import com.flixclusive.feature.mobile.settings.Tweak
//import com.flixclusive.feature.mobile.settings.screen.BaseTweakScreen
//import com.flixclusive.feature.mobile.settings.util.LocalScaffoldNavigator
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.launch
//import com.flixclusive.core.drawables.R as UiCommonR
//import com.flixclusive.core.strings.R as LocaleR
//
//internal object MediaLinksShowDetailTweakScreen : BaseTweakScreen<FlixclusivePrefs> {
//    const val ROUTE = "media_links_show_detail"
//
//    override val isSubNavigation: Boolean = true
//    override val key = stringPreferencesKey(ROUTE)
//
//    override val preferencesAsState: StateFlow<FlixclusivePrefs>
//        get() = throw NotImplementedError("MediaLinksShowDetailTweakScreen does not manage preferences")
//
//    override fun onUpdatePreferences(transform: suspend (FlixclusivePrefs) -> FlixclusivePrefs) = Unit
//
//    @Composable
//    @ReadOnlyComposable
//    override fun getTitle(): String = ""
//
//    @Composable
//    @ReadOnlyComposable
//    override fun getDescription(): String = ""
//
//    @Composable
//    override fun getTweaks(): List<Tweak> = listOf()
//
//    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
//    @Composable
//    override fun Content() {
//        val viewModel = hiltViewModel<MediaLinksTweakViewModel>()
//        val media by viewModel.media.collectAsStateWithLifecycle()
//        val selectedCache by viewModel.selectedCache.collectAsStateWithLifecycle()
//        val navigator = LocalScaffoldNavigator.current
//        val scope = rememberCoroutineScope()
//
//        AsyncAnimatedContent(
//            targetState = media,
//            modifier = Modifier.fillMaxSize(),
//            loadingContent = {
//                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                    CircularProgressIndicator()
//                }
//            },
//            errorContent = {},
//        ) { entries ->
//            val cacheGroup by remember {
//                derivedStateOf {
//                    entries().fastFirstOrNull { it.media.id == selectedCache?.mediaId }
//                }
//            }
//
//            if (cacheGroup == null) {
//                EmptyDataMessage(
//                    modifier = Modifier.fillMaxSize(),
//                    emojiHeader = "🫥",
//                    title = stringResource(LocaleR.string.label_no_cached_media),
//                )
//            } else {
//                val media = cacheGroup!!.media
//                val episodes = cacheGroup!!.cache
//
//                val seasons by remember(episodes) {
//                    derivedStateOf {
//                        cacheGroup
//                            ?.cache
//                            ?.fastMapNotNull { it.seasonNumber }
//                            ?.fastDistinctBy { it }
//                            ?.sorted()
//                            ?: emptyList()
//                    }
//                }
//
//                var selectedSeason by remember(seasons) {
//                    mutableStateOf(seasons.lastOrNull())
//                }
//
//                Column(modifier = Modifier.fillMaxSize()) {
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(horizontal = 12.dp, vertical = 8.dp),
//                        horizontalArrangement = Arrangement.spacedBy(12.dp),
//                        verticalAlignment = Alignment.CenterVertically,
//                    ) {
//                        MediaCover.Poster(
//                            imagePath = media.posterImage,
//                            title = media.title,
//                            modifier = Modifier.width(getAdaptiveMediaCardWidth()),
//                        )
//
//                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
//                            Text(
//                                text = media.title,
//                                style = MaterialTheme.typography.titleMedium,
//                                maxLines = 2,
//                            )
//
//                            Text(
//                                text = "${episodes.size} ${stringResource(LocaleR.string.episodes)}",
//                                style = MaterialTheme.typography.bodySmall,
//                                color = MaterialTheme.colorScheme.onSurfaceVariant,
//                            )
//                        }
//                    }
//
//                    HorizontalDivider()
//
//                    if (seasons.size > 1) {
//                        LazyRow(
//                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
//                            horizontalArrangement = Arrangement.spacedBy(8.dp),
//                        ) {
//                            items(seasons) { season ->
//                                FilterChip(
//                                    selected = selectedSeason == season,
//                                    onClick = { selectedSeason = season },
//                                    label = {
//                                        Text(
//                                            stringResource(LocaleR.string.label_format_untitled_season, season),
//                                        )
//                                    },
//                                )
//                            }
//                        }
//
//                        HorizontalDivider()
//                    }
//
//                    val filteredEpisodes by remember {
//                        derivedStateOf {
//                            if (seasons.isEmpty()) {
//                                episodes
//                            } else {
//                                episodes.fastFilter { it.seasonNumber == selectedSeason }
//                            }
//                        }
//                    }
//
//                    if (filteredEpisodes.isEmpty()) {
//                        EmptyDataMessage(
//                            modifier = Modifier.fillMaxSize(),
//                            emojiHeader = "🫥",
//                            title = stringResource(LocaleR.string.label_no_cached_media),
//                        )
//                    } else {
//                        LazyColumn(modifier = Modifier.fillMaxSize()) {
//                            items(filteredEpisodes, key = { it.cacheWithData.id }) { episode ->
//                                val hasEpisode = episode.episodeNumber != null
//
//                                val label = if (hasEpisode) {
//                                    "S${episode.seasonNumber.toString().padStart(2, '0')}" +
//                                        "E${episode.episodeNumber.toString().padStart(2, '0')}"
//                                } else {
//                                    episode.mediaId
//                                }
//
//                                ListItem(
//                                    headlineContent = { Text(label) },
//                                    supportingContent = {
//                                        Text(
//                                            text = episode.cacheWithData.providerId,
//                                            style = MaterialTheme.typography.bodySmall,
//                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
//                                        )
//                                    },
//                                    trailingContent = {
//                                        Row(
//                                            verticalAlignment = Alignment.CenterVertically,
//                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
//                                        ) {
//                                            Text(
//                                                text = episode.size.toString(),
//                                                style = MaterialTheme.typography.labelSmall,
//                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
//                                            )
//
//                                            Icon(
//                                                painter = painterResource(UiCommonR.drawable.arrow_right_thin),
//                                                contentDescription = null,
//                                            )
//                                        }
//                                    },
//                                    modifier = Modifier.clickable {
//                                        scope.launch {
//                                            navigator?.navigateTo(
//                                                ListDetailPaneScaffoldRole.Detail,
//                                                ManageMediaLinksTweakScreen.ROUTE,
//                                            )
//                                        }
//                                    },
//                                )
//
//                                HorizontalDivider()
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
