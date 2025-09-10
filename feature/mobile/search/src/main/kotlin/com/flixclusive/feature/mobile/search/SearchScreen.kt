package com.flixclusive.feature.mobile.search

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
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.presentation.mobile.components.LARGE_ERROR
import com.flixclusive.core.presentation.mobile.components.RetryButton
import com.flixclusive.core.presentation.mobile.components.topbar.CommonTopBarDefaults.getTopBarHeadlinerTextStyle
import com.flixclusive.core.presentation.mobile.util.LocalGlobalScaffoldPadding
import com.flixclusive.core.strings.UiText
import com.flixclusive.core.ui.common.navigation.navargs.GenreWithBackdrop
import com.flixclusive.core.ui.common.navigation.navigator.GoBackAction
import com.flixclusive.core.ui.common.navigation.navigator.ViewAllFilmsAction
import com.flixclusive.core.ui.common.navigation.navigator.ViewGenreCatalogAction
import com.flixclusive.feature.mobile.search.component.SearchItemCard
import com.flixclusive.feature.mobile.search.component.SearchItemCardPlaceholderWithText
import com.flixclusive.feature.mobile.search.component.SearchItemRow
import com.ramcosta.composedestinations.annotation.Destination
import com.flixclusive.core.strings.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

interface SearchScreenNavigator : GoBackAction, ViewGenreCatalogAction, ViewAllFilmsAction {
    fun openSearchExpandedScreen()
}

@Destination
@Composable
internal fun SearchScreen(
    navigator: SearchScreenNavigator,
    viewModel: SearchScreenViewModel = hiltViewModel(),
) {
    val tvShowNetworkCards by viewModel.tvShowNetworkCards.collectAsStateWithLifecycle()
    val movieCompanyCards by viewModel.movieCompanyCards.collectAsStateWithLifecycle()
    val genreCards by viewModel.genreCards.collectAsStateWithLifecycle()

    LazyVerticalGrid(
        modifier = Modifier.padding(LocalGlobalScaffoldPadding.current),
        columns = GridCells.Adaptive(minSize = 180.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            SearchBarHeader(
                onSearchBarClick = navigator::openSearchExpandedScreen,
            )
        }

        if (viewModel.providersCatalogsCards.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SearchItemRow(
                    list = viewModel.providersCatalogsCards,
                    showItemNames = false,
                    rowTitle = UiText.StringResource(LocaleR.string.browse_providers_catalogs),
                    onClick = navigator::openSeeAllScreen,
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            SearchItemRow(
                list = tvShowNetworkCards,
                showItemNames = false,
                rowTitle = UiText.StringResource(LocaleR.string.browse_tv_networks),
                onClick = navigator::openSeeAllScreen,
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            SearchItemRow(
                list = movieCompanyCards,
                showItemNames = false,
                rowTitle = UiText.StringResource(LocaleR.string.browse_movie_companies),
                onClick = navigator::openSeeAllScreen,
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = stringResource(LocaleR.string.browse_categories),
                style = MaterialTheme.typography.titleMedium,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 15.dp)
                        .padding(top = 15.dp),
            )
        }

        if (genreCards.isLoading) {
            items(20) {
                SearchItemCardPlaceholderWithText()
            }
        } else if (genreCards is Resource.Failure) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                RetryButton(
                    modifier =
                        Modifier
                            .height(LARGE_ERROR),
                    error =
                        genreCards.error?.asString()
                            ?: stringResource(id = LocaleR.string.failed_to_initialize_search_items),
                    onRetry = viewModel::retryLoadingCards,
                )
            }
        } else {
            items(genreCards.data!!) {
                SearchItemCard(
                    image = it.image,
                    label = it.name,
                    onClick = {
                        // If item is not a genre but a film type instead
                        if (it.id < 0) {
                            navigator.openSeeAllScreen(it)
                            return@SearchItemCard
                        }

                        navigator.openGenreScreen(
                            genre =
                                GenreWithBackdrop(
                                    id = it.id,
                                    name = it.name,
                                    posterPath = it.image.toString(),
                                    mediaType = it.mediaType,
                                ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun SearchBarHeader(onSearchBarClick: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(id = LocaleR.string.search),
            style = getTopBarHeadlinerTextStyle(),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 15.dp),
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = TextFieldDefaults.MinHeight)
                    .padding(horizontal = 15.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onSearchBarClick() },
        ) {
            Row(
                modifier =
                    Modifier
                        .padding(horizontal = 15.dp),
                horizontalArrangement = Arrangement.spacedBy(15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(id = UiCommonR.drawable.search_outlined),
                    contentDescription = stringResource(id = LocaleR.string.search),
                )

                Text(
                    text = stringResource(id = LocaleR.string.search_suggestion),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalContentColor.current.copy(0.6f),
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
