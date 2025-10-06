package com.flixclusive.feature.mobile.home.components

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
import com.flixclusive.core.common.pagination.PagingDataState
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.film.FilmCard
import com.flixclusive.core.presentation.mobile.components.film.FilmCardPlaceholder
import com.flixclusive.core.presentation.mobile.extensions.shouldPaginate
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.util.MobileUiUtil.getAdaptiveFilmCardWidth
import com.flixclusive.feature.mobile.home.CatalogPagingState
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.util.FilmType
import com.flixclusive.model.provider.Catalog
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import com.flixclusive.core.presentation.mobile.R as UiMobileR
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun CatalogRow(
    catalog: Catalog,
    pagingState: CatalogPagingState,
    showTitles: Boolean,
    items: PersistentSet<Film>,
    onFilmClick: (Film) -> Unit,
    onFilmLongClick: (Film) -> Unit,
    paginate: () -> Unit,
    onSeeAllItems: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState, paginate, pagingState) {
        snapshotFlow {
            pagingState.hasNext && (listState.shouldPaginate() || items.isEmpty() && pagingState.page == 1)
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
                key = { items.elementAt(it).identifier },
            ) {
                FilmCard(
                    modifier = Modifier.width(getAdaptiveFilmCardWidth()),
                    isShowingTitle = showTitles,
                    film = items.elementAt(it),
                    onClick = onFilmClick,
                    onLongClick = onFilmLongClick,
                )
            }

            if (
                pagingState.state.isLoading ||
                pagingState.state.isError ||
                items.isEmpty()
            ) {
                items(20) {
                    FilmCardPlaceholder(
                        isShowingTitle = showTitles,
                        modifier = Modifier
                            .padding(3.dp)
                            .width(getAdaptiveFilmCardWidth())
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun CatalogRowBasePreview() {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            var items by remember {
                mutableStateOf(
                    List(6) { index ->
                        DummyDataForPreview.getFilm(
                            id = "film_$index",
                            title = "Sample Film ${index + 1}",
                            filmType = if (index % 2 == 0) FilmType.MOVIE else FilmType.TV_SHOW,
                        )
                    }.toPersistentSet(),
                )
            }
            var currentPage by remember { mutableIntStateOf(1) }
            var isLoading by remember { mutableStateOf(false) }
            var requestedPage by remember { mutableIntStateOf(-1) }

            val dummyCatalog = remember {
                object : Catalog() {
                    override val name: String = "Popular Movies"
                    override val url: String = "https://example.com/popular"
                    override val image: String? = null
                    override val canPaginate: Boolean = true
                }
            }

            val pagingState = remember(currentPage, isLoading) {
                CatalogPagingState(
                    hasNext = currentPage < 3, // Simulate max 3 pages
                    page = currentPage,
                    state = when {
                        isLoading -> PagingDataState.Loading
                        currentPage >= 3 -> PagingDataState.Error("End of list")
                        else -> PagingDataState.Success(isExhausted = false)
                    },
                )
            }

            // Handle pagination simulation with LaunchedEffect
            LaunchedEffect(requestedPage) {
                if (requestedPage > 0 && requestedPage <= 3 && !isLoading) {
                    isLoading = true
                    delay(1000) // Simulate loading

                    val newItems = List(6) { index ->
                        val itemIndex = (items.size - 1) + index
                        DummyDataForPreview.getFilm(
                            id = "film_$itemIndex",
                            title = "Sample Film ${itemIndex + 1}",
                            filmType = if (itemIndex % 2 == 0) FilmType.MOVIE else FilmType.TV_SHOW,
                        )
                    }
                    items = items.addAll(newItems)
                    currentPage = requestedPage
                    isLoading = false
                    requestedPage = -1 // Reset
                }
            }

            CatalogRow(
                catalog = dummyCatalog,
                pagingState = pagingState,
                showTitles = true,
                items = items,
                onFilmClick = { },
                onFilmLongClick = { },
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
