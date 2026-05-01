package com.flixclusive.feature.mobile.home.components

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.common.domain.PagingState
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.media.MediaCard
import com.flixclusive.core.presentation.mobile.components.media.MediaCardPlaceholder
import com.flixclusive.core.presentation.mobile.extensions.shouldPaginate
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.util.MobileUiUtil.getAdaptiveMediaCardWidth
import com.flixclusive.feature.mobile.home.CatalogWithPagingState
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.MediaType
import com.flixclusive.model.provider.Catalog
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import com.flixclusive.core.presentation.mobile.R as UiMobileR
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun CatalogRow(
    catalog: Catalog,
    pagingState: PagingState,
    showTitles: Boolean,
    items: List<MediaMetadata>,
    onMediaClick: (MediaMetadata) -> Unit,
    onMediaLongClick: (MediaMetadata) -> Unit,
    paginate: () -> Unit,
    onSeeAllItems: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState, paginate, pagingState) {
        snapshotFlow {
            pagingState.isIdle && (listState.shouldPaginate() || items.isEmpty())
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

        LazyRow(state = listState) {
            items(
                count = items.size,
                key = { items.elementAt(it).id },
            ) {
                MediaCard(
                    modifier = Modifier.width(getAdaptiveMediaCardWidth()),
                    isShowingTitle = showTitles,
                    media = items.elementAt(it),
                    onClick = onMediaClick,
                    onLongClick = onMediaLongClick,
                )
            }

            if (
                pagingState.isLoading ||
                pagingState.isError ||
                items.isEmpty()
            ) {
                items(20) {
                    MediaCardPlaceholder(
                        isShowingTitle = showTitles,
                        modifier = Modifier
                            .padding(3.dp)
                            .width(getAdaptiveMediaCardWidth())
                    )
                }
            }
        }
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
                    }
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

            CatalogRow(
                catalog = dummyCatalog,
                pagingState = pagingState.state,
                showTitles = true,
                items = items,
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
