package com.flixclusive.presentation.mobile.screens.search.content

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.R
import com.flixclusive.common.UiText
import com.flixclusive.domain.model.config.CategoryItem
import com.flixclusive.domain.model.tmdb.Genre
import com.flixclusive.presentation.common.FadeInAndOutScreenTransition
import com.flixclusive.presentation.destinations.SearchGenreScreenDestination
import com.flixclusive.presentation.destinations.SearchScreenExpandedDestination
import com.flixclusive.presentation.destinations.SearchSeeAllScreenDestination
import com.flixclusive.presentation.mobile.common.composables.ErrorScreenWithButton
import com.flixclusive.presentation.mobile.common.composables.LARGE_ERROR
import com.flixclusive.presentation.mobile.main.LABEL_START_PADDING
import com.flixclusive.presentation.mobile.screens.search.SearchNavGraph
import com.flixclusive.presentation.mobile.utils.ComposeMobileUtils.colorOnMediumEmphasisMobile
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@SearchNavGraph(start = true)
@Destination(
    style = FadeInAndOutScreenTransition::class
)
@Composable
fun SearchScreenContent(
    navigator: DestinationsNavigator,
) {
    val viewModel: SearchInitialContentViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    val onItemClick = { item: CategoryItem ->
        navigator.navigate(
            SearchSeeAllScreenDestination(
                item = item
            ),
            onlyIfResumed = true
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 180.dp),
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

        item(span = { GridItemSpan(maxLineSpan) }) {
            SearchItemRow(
                list = viewModel.networks,
                showItemNames = false,
                rowTitle = UiText.StringResource(R.string.browse_tv_networks),
                onClick = onItemClick
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            SearchItemRow(
                list = viewModel.companies,
                showItemNames = false,
                rowTitle = UiText.StringResource(R.string.browse_movie_companies),
                onClick = onItemClick
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = stringResource(R.string.browse_categories),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LABEL_START_PADDING)
                    .padding(top = LABEL_START_PADDING)
            )
        }

        if(state.isLoading && viewModel.genres.isEmpty() && !state.hasErrors) {
            items(20) {
                SearchItemCardPlaceholderWithText()
            }
        }
        else if(state.hasErrors) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ErrorScreenWithButton(
                    modifier = Modifier
                        .height(LARGE_ERROR),
                    shouldShowError = true,
                    error = stringResource(id = R.string.error_on_initialization),
                    onRetry = viewModel::initialize
                )
            }
        }
        else {
            items(viewModel.filmTypes) {
                SearchItemCard(
                    posterPath = it.posterPath,
                    label = it.name,
                    onClick = { onItemClick(it) }
                )
            }

            items(viewModel.genres) {
                SearchItemCard(
                    posterPath = it.posterPath,
                    label = it.name,
                    onClick = {
                        navigator.navigate(
                            direction = SearchGenreScreenDestination(
                                genre = Genre(
                                    id = it.id,
                                    name = it.name,
                                    posterPath = it.posterPath,
                                    mediaType = it.mediaType
                                )
                            ),
                            onlyIfResumed = true
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun SearchBarHeader(
    onSearchBarClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(id = R.string.search),
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
                    painter = painterResource(id = R.drawable.search),
                    contentDescription = stringResource(id = R.string.search)
                )

                Text(
                    text = stringResource(id = R.string.search_suggestion),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorOnMediumEmphasisMobile(),
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}