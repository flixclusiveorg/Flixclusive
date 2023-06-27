package com.flixclusive.presentation.search.content

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.R
import com.flixclusive.domain.model.tmdb.WatchProvider
import com.flixclusive.presentation.common.IconResource
import com.flixclusive.presentation.common.UiText
import com.flixclusive.presentation.common.composables.ErrorScreenWithButton
import com.flixclusive.presentation.common.composables.LARGE_ERROR
import com.flixclusive.presentation.common.composables.placeholderEffect
import com.flixclusive.presentation.common.transitions.ChildScreenTransition
import com.flixclusive.presentation.destinations.SearchGenreScreenDestination
import com.flixclusive.presentation.destinations.SearchScreenExpandedDestination
import com.flixclusive.presentation.destinations.SearchWatchProviderScreenDestination
import com.flixclusive.presentation.main.BOTTOM_NAVIGATION_BAR_PADDING
import com.flixclusive.presentation.main.LABEL_START_PADDING
import com.flixclusive.presentation.search.SearchNavGraph
import com.flixclusive.ui.theme.colorOnMediumEmphasis
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@SearchNavGraph(start = true)
@Destination(
    style = ChildScreenTransition::class
)
@Composable
fun SearchScreenContent(
    navigator: DestinationsNavigator,
) {
    val viewModel: SearchInitialContentViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val genres by viewModel.genres.collectAsStateWithLifecycle()
    val watchProviderItems = remember {
        WatchProvider.values()
            .toList().shuffled()
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 190.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            SearchBarHeader(
                onSearchBarClick = {
                    navigator.navigate(
                        SearchScreenExpandedDestination,
                        onlyIfResumed = true
                    )
                }
            )
        }

        if(genres.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = UiText.StringResource(R.string.browse_networks).asString(),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LABEL_START_PADDING)
                        .padding(top = LABEL_START_PADDING)
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                LazyRow {
                    items(
                        items = watchProviderItems,
                        key = { it.id }
                    ) {
                        WatchProviderItem(
                            modifier = Modifier
                                .height(88.dp)
                                .width(190.dp)
                                .padding(10.dp),
                            item = it,
                            onWatchProviderClick = {
                                navigator.navigate(
                                    SearchWatchProviderScreenDestination(
                                        item = it
                                    ),
                                    onlyIfResumed = true
                                )
                            }
                        )
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = UiText.StringResource(R.string.browse_genres).asString(),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LABEL_START_PADDING)
                        .padding(top = LABEL_START_PADDING)
                )
            }

            items(genres, key = { it.id }) {
                GenreItem(
                    modifier = Modifier
                        .height(130.dp)
                        .width(190.dp)
                        .padding(10.dp),
                    item = it,
                    onGenreClick = {
                        navigator.navigate(
                            SearchGenreScreenDestination(
                                genre = it
                            ),
                            onlyIfResumed = true
                        )
                    }
                )
            }
        }

        if(state.isLoading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Spacer(
                        modifier = Modifier
                            .height(31.dp)
                            .width(145.dp)
                            .padding(horizontal = LABEL_START_PADDING)
                            .padding(top = LABEL_START_PADDING)
                            .placeholderEffect()
                    )

                    LazyRow {
                        items(3) {
                            WatchHistoryItemPlaceholder()
                        }
                    }

                    Spacer(
                        modifier = Modifier
                            .height(31.dp)
                            .width(150.dp)
                            .padding(horizontal = LABEL_START_PADDING)
                            .padding(top = LABEL_START_PADDING)
                            .placeholderEffect()
                    )
                }
            }

            items(20) {
                GenreItemPlaceholder()
            }
        }

        if(state.hasErrors) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ErrorScreenWithButton(
                    modifier = Modifier
                        .height(LARGE_ERROR),
                    shouldShowError = true,
                    error = UiText.StringResource(R.string.error_on_initialization).asString(),
                    onRetry = viewModel::initialize
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(
                modifier = Modifier
                    .padding(bottom = BOTTOM_NAVIGATION_BAR_PADDING)
            )
        }
    }
}

@Composable
fun SearchBarHeader(
    onSearchBarClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = UiText.StringResource(R.string.search).asString(),
            style = MaterialTheme.typography.headlineMedium,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = LABEL_START_PADDING)
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = TextFieldDefaults.MinHeight)
                .padding(horizontal = LABEL_START_PADDING)
                .graphicsLayer {
                    shadowElevation = 8.dp.toPx()
                    shape = RoundedCornerShape(55)
                    clip = true
                }
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onSearchBarClick() }
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 15.dp),
                horizontalArrangement = Arrangement.spacedBy(15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = IconResource.fromDrawableResource(R.drawable.search)
                        .asPainterResource(),
                    contentDescription = UiText.StringResource(R.string.search).asString()
                )

                Text(
                    text = UiText.StringResource(R.string.search_suggestion).asString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorOnMediumEmphasis(),
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}