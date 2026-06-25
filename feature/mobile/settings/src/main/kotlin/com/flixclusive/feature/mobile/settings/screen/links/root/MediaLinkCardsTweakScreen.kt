package com.flixclusive.feature.mobile.settings.screen.links.root

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.common.domain.Async.Companion.AsyncAnimatedContent
import com.flixclusive.core.navigation.navigator.NavigateBack
import com.flixclusive.core.navigation.navigator.NavigateToManageMediaLinksScreen
import com.flixclusive.core.navigation.navigator.NavigateToMediaPreviewBottomSheet
import com.flixclusive.core.presentation.common.components.MediaCover
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.common.util.ViewModelUtil.activityHiltViewModel
import com.flixclusive.core.presentation.mobile.components.EmptyDataMessage
import com.flixclusive.core.presentation.mobile.components.RetryButton
import com.flixclusive.core.presentation.mobile.components.material3.topbar.rememberEnterAlwaysScrollBehavior
import com.flixclusive.core.presentation.mobile.components.media.MediaCard
import com.flixclusive.core.presentation.mobile.components.media.MediaCardPlaceholder
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.MobileUiUtil.getAdaptiveMediaCardWidth
import com.flixclusive.feature.mobile.settings.R
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.MediaType
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph
import androidx.compose.foundation.lazy.grid.items as gridItems
import com.flixclusive.core.strings.R as LocaleR

interface NavigatorMediaLinkCardsTweakScreen :
    NavigateBack,
    NavigateToMediaPreviewBottomSheet,
    NavigateToManageMediaLinksScreen {
    fun navigateToManageShowLinksScreen(media: MediaMetadata)
}

@Destination<ExternalModuleGraph>
@Composable
internal fun MediaLinkCardsTweakScreen(
    navigator: NavigatorMediaLinkCardsTweakScreen,
    viewModel: MediaLinkCardsTweakViewModel = activityHiltViewModel()
) {
    val cacheList by viewModel.cacheList.collectAsStateWithLifecycle()
    val mediaSort by viewModel.mediaSort.collectAsStateWithLifecycle()
    val showMediaTitles by viewModel.showMediaTitles.collectAsStateWithLifecycle()
    val scrollBehavior = rememberEnterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediaLinkCardsTweakTopBar(
                mediaSort = mediaSort,
                onMediaSortChange = viewModel::onMediaSortChange,
                navigateBack = navigator::navigateBack,
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        AsyncAnimatedContent(
            targetState = cacheList,
            modifier = Modifier.fillMaxSize(),
            loadingContent = { LoadingScreen(innerPadding) },
            errorContent = {
                RetryButton(
                    error = remember(it.cause) { it.cause?.stackTraceToString() },
                    onRetry = viewModel::initialize,
                    modifier = Modifier.padding(innerPadding)
                )
            },
        ) { items ->
            val filteredItems by remember {
                derivedStateOf {
                    items()
                        .let { list ->
                            when (mediaSort) {
                                is MediaSortType.LinksCount -> {
                                    if (mediaSort.asc) {
                                        list.sortedBy { it.size }
                                    } else {
                                        list.sortedByDescending { it.size }
                                    }
                                }

                                is MediaSortType.Title -> {
                                    if (mediaSort.asc) {
                                        list.sortedBy { it.media.title }
                                    } else {
                                        list.sortedByDescending { it.media.title }
                                    }
                                }
                            }
                        }
                }
            }

            if (items().isEmpty()) {
                EmptyDataMessage(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    emojiHeader = "🫥",
                    title = stringResource(LocaleR.string.label_no_cached_media),
                )
                return@AsyncAnimatedContent
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(getAdaptiveMediaCardWidth()),
                contentPadding = innerPadding,
                modifier = Modifier.fillMaxSize(),
            ) {
                gridItems(
                    items = filteredItems,
                    key = { it.media.id }
                ) { item ->
                    PosterCard(
                        media = item.media,
                        cacheSize = item.size,
                        isShowingTitle = showMediaTitles,
                        onLongClick = {
                            navigator.showMediaPreviewBottomSheet(
                                media = item.media
                            )
                        },
                        onClick = {
                            if (item.media.type == MediaType.MOVIE) {
                                navigator.navigateToManageMediaLinksScreen(media = item.media)
                            } else {
                                navigator.navigateToManageShowLinksScreen(media = item.media)
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
private fun LoadingScreen(
    scaffoldPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(getAdaptiveMediaCardWidth()),
        contentPadding = scaffoldPadding,
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
    media: MediaMetadata,
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
            media = remember { media },
            onClick = { onClick() },
            onLongClick = { onLongClick() },
            isShowingTitle = isShowingTitle,
            modifier = Modifier.fillMaxWidth(),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(MediaCover.Poster.ratio)
                .padding(3.dp)
                .clip(MaterialTheme.shapes.small)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.7f to Color.Black.copy(alpha = 0.4f),
                        1f to Color.Black
                    )
                )
        ) {
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
}

@Preview
@Composable
private fun PosterCardPreview() {
    FlixclusiveTheme {
        Surface {
            PosterCard(
                media = DummyDataForPreview.getMedia(),
                cacheSize = 1,
                isShowingTitle = true,
                onClick = {},
                onLongClick = {},
                modifier = Modifier.width(getAdaptiveMediaCardWidth())
            )
        }
    }
}
