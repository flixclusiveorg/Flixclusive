package com.flixclusive.feature.mobile.settings.screen.links

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.common.domain.Async.Companion.AsyncAnimatedContent
import com.flixclusive.core.database.entity.media.DBMedia
import com.flixclusive.core.database.entity.media.DBMedia.Companion.toDBMedia
import com.flixclusive.core.database.entity.media.DBMedia.Companion.toMediaMetadata
import com.flixclusive.core.datastore.model.FlixclusivePrefs
import com.flixclusive.core.presentation.common.extensions.showToast
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.EmptyDataMessage
import com.flixclusive.core.presentation.mobile.components.RetryButton
import com.flixclusive.core.presentation.mobile.components.media.MediaCard
import com.flixclusive.core.presentation.mobile.components.media.MediaCardPlaceholder
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.MobileUiUtil.getAdaptiveMediaCardWidth
import com.flixclusive.feature.mobile.preferences.R
import com.flixclusive.feature.mobile.settings.Tweak
import com.flixclusive.feature.mobile.settings.screen.BaseTweakScreen
import com.flixclusive.feature.mobile.settings.util.LocalScaffoldNavigator
import com.flixclusive.feature.mobile.settings.util.LocalSettingsNavigator
import com.flixclusive.model.media.common.MediaType
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.grid.items as gridItems
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

internal object MediaLinkCardsTweakScreen : BaseTweakScreen<FlixclusivePrefs> {
    override val isSubNavigation: Boolean = true
    override val key = stringPreferencesKey("media_links_tweak")

    override val preferencesAsState: StateFlow<FlixclusivePrefs>
        get() = throw NotImplementedError("MediaLinkCardsTweakScreen does not manage preferences")

    override fun onUpdatePreferences(transform: suspend (FlixclusivePrefs) -> FlixclusivePrefs) = Unit

    @Composable
    @ReadOnlyComposable
    override fun getTitle(): String = stringResource(LocaleR.string.label_manage_cached_links)

    @Composable
    @ReadOnlyComposable
    override fun getDescription(): String = stringResource(LocaleR.string.desc_manage_cached_links_content_desc)

    @Composable
    override fun getIconPainter(): Painter = painterResource(UiCommonR.drawable.database_icon_thin)

    @Composable
    override fun getTweaks(): List<Tweak> = listOf()

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    @Composable
    override fun Content() {
        val viewModel = hiltViewModel<MediaLinksTweakViewModel>()
        val media by viewModel.media.collectAsStateWithLifecycle()
        val mediaSort by viewModel.mediaSort.collectAsStateWithLifecycle()
        val showMediaTitles by viewModel.showMediaTitles.collectAsStateWithLifecycle()
        val navigator = LocalScaffoldNavigator.current
        val settingsNavigator = LocalSettingsNavigator.current

        val context = LocalContext.current
        val resources = LocalResources.current
        val scope = rememberCoroutineScope()

        AsyncAnimatedContent(
            targetState = media,
            loadingContent = { LoadingScreen() },
            errorContent = {
                RetryButton(
                    error = remember(it.cause) { it.cause?.stackTraceToString() },
                    onRetry = viewModel::initialize
                )
            }
        ) { items ->
            val filteredItems by remember {
                derivedStateOf {
                    items()
                        .let { list ->
                            when (mediaSort) {
                                is MediaSortType.LinksCount -> {
                                    if (mediaSort.asc) list.sortedBy { it.size }
                                    else list.sortedByDescending { it.size }
                                }
                                is MediaSortType.Title -> {
                                    if (mediaSort.asc) list.sortedBy { it.media.title }
                                    else list.sortedByDescending { it.media.title }
                                }
                            }
                        }
                }
            }

            if (items().isEmpty()) {
                EmptyDataMessage(
                    modifier = Modifier.fillMaxSize(),
                    emojiHeader = "🫥",
                    title = stringResource(LocaleR.string.label_no_cached_media),
                )
                return@AsyncAnimatedContent
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(getAdaptiveMediaCardWidth()),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    val sorts = remember {
                        listOf(
                            MediaSortType.LinksCount::class,
                            MediaSortType.Title::class
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        sorts.fastForEach {
                            val isSelected = mediaSort::class == it

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    viewModel.onMediaSortChange(
                                        if (isSelected) mediaSort.toggle()
                                        else mediaSort.changeType()
                                    )
                                },
                                leadingIcon = {
                                    AnimatedVisibility(
                                        visible = isSelected,
                                        enter = fadeIn(),
                                        exit = fadeOut()
                                    ) {
                                        AnimatedContent(targetState = mediaSort.asc) { state ->
                                            val iconId = if (state) {
                                                UiCommonR.drawable.sort_ascending
                                            } else {
                                                UiCommonR.drawable.sort_descending
                                            }

                                            AdaptiveIcon(
                                                painter = painterResource(iconId),
                                                contentDescription = stringResource(LocaleR.string.sort_icon_content_desc),
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                dp = 14.dp,
                                            )
                                        }
                                    }
                                },
                                label = {
                                    Text(
                                        when (it) {
                                            MediaSortType.LinksCount::class -> stringResource(R.string.label_links_filter_count)
                                            MediaSortType.Title::class -> stringResource(R.string.label_links_filter_title)
                                            else -> "Unknown filter"
                                        }
                                    )
                                },
                            )
                        }
                    }
                }

                gridItems(
                    items = filteredItems,
                    key = { it.media.id }
                ) { group ->
                    PosterCard(
                        media = group.media,
                        cacheSize = group.size,
                        isShowingTitle = showMediaTitles,
                        onLongClick = {
                            settingsNavigator?.showMediaPreviewBottomSheet(
                                media = group.media.toMediaMetadata(emptyMap())
                            )
                        },
                        onClick = {
                            val providerCache = group.cache.firstOrNull()
                            if (providerCache == null) {
                                context.showToast(resources.getString(R.string.error_selected_media_has_no_cached_links))
                                return@PosterCard
                            }

                            viewModel.selectCache(providerCache.cacheWithData.cache)
                            scope.launch {
                                if (group.media.type == MediaType.MOVIE) {
                                    navigator?.navigateTo(
                                        ListDetailPaneScaffoldRole.Detail,
                                        ManageMediaLinksTweakScreen.ROUTE,
                                    )
                                } else {
                                    navigator?.navigateTo(
                                        ListDetailPaneScaffoldRole.Detail,
                                        MediaLinksShowDetailTweakScreen.ROUTE,
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen(modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(getAdaptiveMediaCardWidth()),
        modifier = modifier.fillMaxSize(),
    ) {
        items(5) {
            MediaCardPlaceholder(
                modifier = Modifier
                    .padding(3.dp)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PosterCard(
    media: DBMedia,
    cacheSize: Int,
    isShowingTitle: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable {
                onClick()
            }
    ) {
        MediaCard(
            media = remember(media) { media.toMediaMetadata(emptyMap()) },
            onClick = { onClick() },
            onLongClick = { onLongClick() },
            isShowingTitle = isShowingTitle,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.7f to Color.Black.copy(alpha = 0.4f),
                        1f to Color.Black
                    )
                )
        )

        Text(
            stringResource(R.string.label_links_size, cacheSize),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
        )
    }
}

@Preview
@Composable
private fun PosterCardPreview() {
    FlixclusiveTheme {
        Surface(
        ) {
            PosterCard(
                media = DummyDataForPreview.getMedia().toDBMedia(),
                cacheSize = 1,
                isShowingTitle = true,
                onClick = {},
                onLongClick = {},
                modifier = Modifier.width(getAdaptiveMediaCardWidth())
            )
        }
    }
}
