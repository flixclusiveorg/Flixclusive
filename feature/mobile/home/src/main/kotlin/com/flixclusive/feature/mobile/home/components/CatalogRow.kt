package com.flixclusive.feature.mobile.home.components

import android.annotation.SuppressLint
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.common.domain.PagingState
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.presentation.common.components.MediaCover
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.EmptyDataMessage
import com.flixclusive.core.presentation.mobile.components.media.MediaCard
import com.flixclusive.core.presentation.mobile.components.media.MediaCardPlaceholder
import com.flixclusive.core.presentation.mobile.extensions.shouldPaginate
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.util.MobileUiUtil.getAdaptiveMediaCardWidth
import com.flixclusive.feature.mobile.home.CatalogWithPagingState
import com.flixclusive.feature.mobile.home.R
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.MediaType
import com.flixclusive.model.provider.Catalog
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import com.flixclusive.core.presentation.mobile.R as UiMobileR
import com.flixclusive.core.strings.R as LocaleR

private enum class CatalogRowState {
    CONTENT,
    EMPTY,
    ERROR
}

@Composable
internal fun CatalogRow(
    catalog: Catalog,
    pagingState: PagingState,
    showTitles: Boolean,
    items: () -> Set<MediaMetadata>,
    onMediaClick: (MediaMetadata) -> Unit,
    onMediaLongClick: (MediaMetadata) -> Unit,
    paginate: () -> Unit,
    onSeeAllItems: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    val uiState by remember(pagingState) {
        derivedStateOf {
            when {
                pagingState.isError && items().isEmpty() -> CatalogRowState.ERROR
                items().isEmpty() && pagingState.isExhausted -> CatalogRowState.EMPTY
                else -> CatalogRowState.CONTENT
            }
        }
    }

    LaunchedEffect(listState, paginate, pagingState, items) {
        snapshotFlow {
            pagingState.isIdle && (listState.shouldPaginate() || items().isEmpty())
        }.distinctUntilChanged()
            .filter { it }
            .collect {
                paginate()
            }
    }

    Column(
        modifier = modifier
            .padding(vertical = if (showTitles) 3.dp else 8.dp),
    ) {
        Box(
            modifier = Modifier
                .clickable {
                    onSeeAllItems()
                },
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .padding(vertical = 10.dp),
            ) {
                Text(
                    text = catalog.name,
                    style = MaterialTheme.typography.labelLarge.asAdaptiveTextStyle(
                        size = 16.sp,
                        increaseBy = 10.sp
                    ),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .weight(1F)
                        .padding(start = 15.dp),
                )

                Box(
                    modifier = Modifier
                        .padding(end = 15.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AdaptiveIcon(
                        painter = painterResource(id = UiMobileR.drawable.right_arrow),
                        contentDescription = stringResource(id = LocaleR.string.see_all),
                        tint = LocalContentColor.current.copy(0.6f),
                        dp = 14.dp,
                        increaseBy = 6.dp,
                        modifier = Modifier
                            .clickable { onSeeAllItems() },
                    )
                }
            }
        }

        Crossfade(targetState = uiState) { state ->
            when (state) {
                CatalogRowState.ERROR -> {
                    ErrorCatalogRow(
                        onRetry = { paginate() },
                        error = (pagingState as? PagingState.Error)?.error,
                    )
                }

                CatalogRowState.EMPTY -> {
                    EmptyCatalogRow()
                }

                else -> {
                    LazyRow(state = listState) {
                        items(
                            count = items().size,
                            key = { items().elementAt(it).id },
                        ) {
                            val item by remember {
                                derivedStateOf {
                                    items().elementAt(it)
                                }
                            }

                            MediaCard(
                                modifier = Modifier.width(getAdaptiveMediaCardWidth()),
                                isShowingTitle = showTitles,
                                media = item,
                                onClick = onMediaClick,
                                onLongClick = onMediaLongClick,
                            )
                        }

                        if (pagingState.isLoading || items().isEmpty()) {
                            items(10) {
                                MediaCardPlaceholder(
                                    isShowingTitle = showTitles,
                                    modifier = Modifier
                                        .animateItem()
                                        .padding(3.dp)
                                        .width(getAdaptiveMediaCardWidth())
                                )
                            }
                        }

                        if (pagingState.isError) {
                            item {
                                ErrorCatalogRow(
                                    onRetry = { paginate() },
                                    error = (pagingState as? PagingState.Error)?.error,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorCatalogRow(
    error: UiText?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val defaultLabel = stringResource(LocaleR.string.unknown_season)
    val message = remember { error?.asString(context) ?: defaultLabel }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(3.dp)
            .background(
                color = MaterialTheme.colorScheme.error.copy(0.1f),
                shape = MaterialTheme.shapes.small
            ).border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.error.copy(0.6f),
                shape = MaterialTheme.shapes.small
            )
    ) {
        Spacer(
            modifier = Modifier
                .width(getAdaptiveMediaCardWidth())
                .aspectRatio(MediaCover.Poster.ratio)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .align(Alignment.Center)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            EmptyDataMessage(
                title = stringResource(UiMobileR.string.an_error_occurred),
                description = message,
                icon = {},
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onRetry,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = stringResource(LocaleR.string.retry),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun EmptyCatalogRow(modifier: Modifier = Modifier) {
    val borderColor = MaterialTheme.colorScheme.onSurface.copy(0.2f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(3.dp)
            .drawBehind {
                val stroke = Stroke(
                    width = 6f,
                    pathEffect = PathEffect.dashPathEffect(
                        intervals = floatArrayOf(30f, 50f),
                        phase = 0f
                    )
                )

                drawRoundRect(
                    color = borderColor,
                    style = stroke,
                    cornerRadius = CornerRadius(8f, 8f)
                )
            }
    ) {
        Spacer(
            modifier = Modifier
                .width(getAdaptiveMediaCardWidth())
                .aspectRatio(MediaCover.Poster.ratio)
        )

        EmptyDataMessage(
            emojiHeader = "🫗",
            title = stringResource(R.string.empty_catalog_title),
            description = stringResource(R.string.empty_catalog_desc),
            modifier = Modifier
                .padding(horizontal = 15.dp)
                .align(Alignment.Center)
        )
    }
}

@SuppressLint("MutableCollectionMutableState")
@Preview
@Composable
private fun CatalogRowBasePreview() {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            var items by remember {
                mutableStateOf(
                    MutableList(6) { index ->
                        DummyDataForPreview.getMedia(
                            id = "media_$index",
                            title = "Sample MediaMetadata ${index + 1}",
                            mediaType = if (index % 2 == 0) MediaType.MOVIE else MediaType.SHOW,
                        )
                    }.toSet()
                )
            }
            var currentPage by remember { mutableIntStateOf(1) }
            var isLoading by remember { mutableStateOf(false) }
            var requestedPage by remember { mutableIntStateOf(-1) }

            val dummyCatalog = remember {
                Catalog(
                    name = "Dummy Catalog",
                    url = "https://example.com/catalog",
                    image = null,
                    canPaginate = true,
                    providerId = "dummy_provider",
                )
            }

            val pagingState = remember(currentPage, isLoading) {
                CatalogWithPagingState(
                    page = currentPage,
                    catalog = dummyCatalog,
                    medias = items,
                    state = when {
                        isLoading -> PagingState.Loading
                        currentPage >= 3 -> PagingState.Error("End of list")
                        else -> PagingState.Exhausted
                    },
                )
            }

            // Handle pagination simulation with LaunchedEffect
            LaunchedEffect(requestedPage) {
                if (requestedPage in 1..3 && !isLoading) {
                    isLoading = true
                    delay(1000) // Simulate loading

                    val newItems = List(6) { index ->
                        val itemIndex = (items.size - 1) + index
                        DummyDataForPreview.getMedia(
                            id = "media_$itemIndex",
                            title = "Sample MediaMetadata ${itemIndex + 1}",
                            mediaType = if (itemIndex % 2 == 0) MediaType.MOVIE else MediaType.SHOW,
                        )
                    }
                    items += newItems
                    currentPage = requestedPage
                    isLoading = false
                    requestedPage = -1 // Reset
                }
            }

            val error = remember {
                PagingState.Error(
                    UiText.from(
                        "Failed to load data. Please try again."
                    )
                )
            }

            Column {
                CatalogRow(
                    catalog = dummyCatalog,
                    pagingState = error,
                    showTitles = true,
                    items = { emptySet() },
                    onMediaClick = { },
                    onMediaLongClick = { },
                    onSeeAllItems = { },
                    paginate = {
                        if (!isLoading && requestedPage <= 3) {
                            requestedPage += 1
                        }
                    },
                )

                CatalogRow(
                    catalog = dummyCatalog,
                    pagingState = error,
                    showTitles = true,
                    items = { items },
                    onMediaClick = { },
                    onMediaLongClick = { },
                    onSeeAllItems = { },
                    paginate = {
                        if (!isLoading && requestedPage <= 3) {
                            requestedPage += 1
                        }
                    },
                )

                CatalogRow(
                    catalog = dummyCatalog,
                    pagingState = pagingState.state,
                    showTitles = true,
                    items = { emptySet() },
                    onMediaClick = { },
                    onMediaLongClick = { },
                    onSeeAllItems = { },
                    paginate = {
                        if (!isLoading && requestedPage <= 3) {
                            requestedPage += 1
                        }
                    },
                )
            }
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun CatalogRowCompactLandscapePreview() {
    CatalogRowBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun CatalogRowMediumPortraitPreview() {
    CatalogRowBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun CatalogRowMediumLandscapePreview() {
    CatalogRowBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun CatalogRowExtendedPortraitPreview() {
    CatalogRowBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun CatalogRowExtendedLandscapePreview() {
    CatalogRowBasePreview()
}
