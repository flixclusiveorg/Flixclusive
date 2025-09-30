package com.flixclusive.feature.mobile.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.presentation.common.components.FilmCover
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.components.RetryButton
import com.flixclusive.core.presentation.mobile.components.material3.topbar.CommonTopBarDefaults.getTopBarHeadlinerTextStyle
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.LocalGlobalScaffoldPadding
import com.flixclusive.core.presentation.mobile.util.copy
import com.flixclusive.data.tmdb.model.TMDBDiscoverCatalog
import com.flixclusive.domain.catalog.model.DiscoverCards
import com.flixclusive.feature.mobile.search.component.DiscoverCard
import com.flixclusive.feature.mobile.search.component.DiscoverCardPlaceholder
import com.flixclusive.feature.mobile.search.component.DiscoverRow
import com.flixclusive.feature.mobile.search.util.SearchUiUtils
import com.flixclusive.model.provider.Catalog
import com.flixclusive.model.provider.ProviderCatalog
import com.ramcosta.composedestinations.annotation.Destination
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

// TODO: Remove this screen in the future and use only SearchExpandedScreen

@Destination
@Composable
internal fun SearchScreen(
    navigator: SearchScreenNavigator,
    viewModel: SearchScreenViewModel = hiltViewModel(),
) {
    val cards by viewModel.cards.collectAsStateWithLifecycle()

    SearchScreenContent(
        cards = cards,
        providerCards = viewModel.providersCatalogsCards,
        openSearchExpandedScreen = navigator::openSearchExpandedScreen,
        onRetryLoadingCards = viewModel::initializeCards,
        openSeeAllScreen = navigator::openSeeAllScreen,
    )
}

@Composable
private fun SearchScreenContent(
    cards: Resource<DiscoverCards>,
    providerCards: List<ProviderCatalog>,
    onRetryLoadingCards: () -> Unit,
    openSearchExpandedScreen: () -> Unit,
    openSeeAllScreen: (Catalog) -> Unit,
) {
    val tvShowNetworkCards = remember(cards) {
        cards.data?.tvNetworks ?: emptyList()
    }
    val movieCompanyCards = remember(cards) {
        cards.data?.movieCompanies ?: emptyList()
    }
    val categoryCards = remember(cards) {
        cards.data?.categories ?: emptyList()
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = SearchUiUtils.getCardWidth(170.dp)),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = LocalGlobalScaffoldPadding.current.copy(
            start = 15.dp,
            end = 15.dp,
        ),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            SearchBarHeader(
                onSearchBarClick = openSearchExpandedScreen,
            )
        }

        if (providerCards.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                DiscoverRow(
                    list = providerCards,
                    rowTitle = UiText.from(LocaleR.string.browse_providers_catalogs),
                ) {
                    DiscoverCard(
                        label = it.name,
                        image = it.image,
                        isProviderCatalog = true,
                        onClick = { openSeeAllScreen(it) },
                        modifier = Modifier.width(SearchUiUtils.getCardWidth()),
                    )
                }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            DiscoverRow(
                list = tvShowNetworkCards,
                rowTitle = UiText.StringResource(LocaleR.string.browse_tv_networks),
            ) {
                DiscoverCard(
                    label = it.name,
                    image = it.image,
                    imageSize = "w500_filter(negate,000,666)",
                    isCompanyCatalog = true,
                    onClick = { openSeeAllScreen(it) },
                    modifier = Modifier.width(SearchUiUtils.getCardWidth()),
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            DiscoverRow(
                list = movieCompanyCards,
                rowTitle = UiText.StringResource(LocaleR.string.browse_movie_companies),
            ) {
                DiscoverCard(
                    label = it.name,
                    image = it.image,
                    imageSize = "w500_filter(negate,000,666)",
                    isCompanyCatalog = true,
                    onClick = { openSeeAllScreen(it) },
                    modifier = Modifier.width(SearchUiUtils.getCardWidth()),
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = stringResource(LocaleR.string.browse_categories),
                style = MaterialTheme.typography.labelLarge.asAdaptiveTextStyle(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 15.dp),
            )
        }

        if (cards !is Resource.Failure) {
            items(categoryCards) {
                DiscoverCard(
                    image = it.image,
                    label = it.name,
                    onClick = { openSeeAllScreen(it) },
                )
            }

            items(10) {
                DiscoverCardPlaceholder()
            }
        } else {
            item(span = { GridItemSpan(maxLineSpan) }) {
                RetryButton(
                    modifier = Modifier.aspectRatio(FilmCover.Backdrop.ratio),
                    error = cards.error?.asString()
                        ?: stringResource(R.string.failed_to_fetch_cards),
                    onRetry = onRetryLoadingCards,
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
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
        )

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = TextFieldDefaults.MinHeight)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onSearchBarClick() },
        ) {
            Spacer(modifier = Modifier.width(15.dp))

            Icon(
                painter = painterResource(id = UiCommonR.drawable.search_outlined),
                contentDescription = stringResource(id = LocaleR.string.search),
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = stringResource(id = LocaleR.string.search_suggestion),
                style = MaterialTheme.typography.bodyMedium,
                color = LocalContentColor.current.copy(0.6f),
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            Spacer(modifier = Modifier.width(15.dp))
        }
    }
}

@Preview
@Composable
private fun SearchScreenBasePreview() {
    val tvShowNetworkCards = List(10) {
        TMDBDiscoverCatalog(
            name = "Network $it",
            image = null,
            url = "$it",
        )
    }
    val movieCompanyCards = List(10) {
        TMDBDiscoverCatalog(
            name = "Company $it",
            image = null,
            url = "$it",
        )
    }
    val genreCards = List(10) {
        TMDBDiscoverCatalog(
            name = "Genre $it",
            image = null,
            url = "$it",
        )
    }
    val providerCards = List(10) {
        ProviderCatalog(
            name = "Provider $it",
            image = null,
            url = "$it",
            canPaginate = true,
            providerId = "$it",
        )
    }

    FlixclusiveTheme {
        Surface {
            SearchScreenContent(
                cards = remember {
                    Resource.Success(
                        DiscoverCards(
                            tvNetworks = tvShowNetworkCards,
                            movieCompanies = movieCompanyCards,
                            categories = genreCards,
                        ),
                    )
                },
                providerCards = providerCards,
                openSearchExpandedScreen = {},
                onRetryLoadingCards = {},
                openSeeAllScreen = {},
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun SearchScreenCompactLandscapePreview() {
    SearchScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun SearchScreenMediumPortraitPreview() {
    SearchScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun SearchScreenMediumLandscapePreview() {
    SearchScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun SearchScreenExtendedPortraitPreview() {
    SearchScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun SearchScreenExtendedLandscapePreview() {
    SearchScreenBasePreview()
}
