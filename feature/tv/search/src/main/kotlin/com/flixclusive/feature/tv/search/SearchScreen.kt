package com.flixclusive.feature.tv.search

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.itemsIndexed
import androidx.tv.foundation.lazy.grid.rememberTvLazyGridState
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.ui.common.navigation.navigator.CommonScreenNavigator
import com.flixclusive.core.ui.common.util.PagingState
import com.flixclusive.core.ui.common.util.ifElse
import com.flixclusive.core.ui.tv.component.FilmCard
import com.flixclusive.core.ui.tv.util.LabelStartPadding
import com.flixclusive.core.ui.tv.util.LocalDirectionalFocusRequesterProvider
import com.flixclusive.core.ui.tv.util.LocalFocusTransferredOnLaunchProvider
import com.flixclusive.core.ui.tv.util.createDefaultFocusRestorerModifier
import com.flixclusive.core.ui.tv.util.createInitialFocusRestorerModifiers
import com.flixclusive.core.ui.tv.util.focusOnMount
import com.flixclusive.core.ui.tv.util.getLocalDrawerWidth
import com.flixclusive.core.ui.tv.util.shouldPaginate
import com.flixclusive.core.ui.tv.util.useLocalCurrentRoute
import com.flixclusive.core.ui.tv.util.useLocalDirectionalFocusRequester
import com.flixclusive.core.ui.tv.util.useLocalLastFocusedItemPerDestination
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.feature.tv.search.component.FilterBlock
import com.flixclusive.feature.tv.search.component.KEYBOARD_FOCUS_KEY_FORMAT
import com.flixclusive.feature.tv.search.component.SearchCustomKeyboard
import com.flixclusive.feature.tv.search.component.SuggestionBlock
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.flixclusive.core.ui.common.R as UiCommonR

@OptIn(ExperimentalTvMaterial3Api::class)
@Destination
@Composable
internal fun SearchScreen(
    navigator: CommonScreenNavigator
) {
    val viewModel: SearchScreenViewModel = hiltViewModel()
    val categories by viewModel.catalogs.collectAsStateWithLifecycle()


    var lastSearchedQuery by remember { mutableStateOf(viewModel.searchQuery) }

    val scope = rememberCoroutineScope()
    val listState = rememberTvLazyGridState()
    val shouldStartPaginate by remember {
        derivedStateOf {
            viewModel.canPaginate && listState.shouldPaginate()
        }
    }

    LaunchedEffect(shouldStartPaginate) {
        if(shouldStartPaginate && (viewModel.pagingState == PagingState.IDLE || viewModel.pagingState == PagingState.ERROR))
            viewModel.paginate()
    }


    LaunchedEffect(viewModel.searchQuery, lastSearchedQuery) {
        val queryIsNotEmpty = viewModel.searchQuery.isNotEmpty()
        val userIsTypingNewQuery = viewModel.searchQuery != lastSearchedQuery

        if(queryIsNotEmpty && userIsTypingNewQuery) {
            delay(1500L)
            safeCall { listState.scrollToItem(0) }
            viewModel.onSearch()
        }

        lastSearchedQuery = viewModel.searchQuery
    }

    LaunchedEffect(viewModel.selectedCatalog?.url) {
        safeCall { listState.scrollToItem(0) }
        viewModel.onSearch()
    }

    val lastFocusedItems = useLocalLastFocusedItemPerDestination()
    val currentRoute = useLocalCurrentRoute()
    LaunchedEffect(Unit) {
        lastFocusedItems.getOrPut(currentRoute) {
            String.format(KEYBOARD_FOCUS_KEY_FORMAT, "a")
        }
    }

    LocalFocusTransferredOnLaunchProvider {
        LocalDirectionalFocusRequesterProvider {
            val filtersGroupFocusRequester = useLocalDirectionalFocusRequester().right
            
            Row(
                modifier = Modifier
                    .focusGroup()
                    .padding(
                        top = 20.dp,
                        start = LabelStartPadding.start + getLocalDrawerWidth()
                    ),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .focusGroup()
                        .weight(0.35F)
                        .fillMaxHeight()
                ) {
                    val focusRestorersModifiers = createInitialFocusRestorerModifiers()

                    SearchCustomKeyboard(
                        currentSearchQuery = viewModel.searchQuery,
                        onKeyboardClick = {
                            val newQuery = viewModel.searchQuery + it
                            viewModel.onQueryChange(query = newQuery)
                        },
                        onBackspaceClick = {
                            viewModel.onQueryChange(query = viewModel.searchQuery.dropLast(1))
                        },
                        onBackspaceLongClick = { viewModel.onQueryChange("") },
                        modifier = Modifier
                            .focusGroup()
                    )

                    TvLazyColumn(
                        pivotOffsets = PivotOffsets(0F, 0F),
                        modifier = focusRestorersModifiers.parentModifier
                            .fillMaxWidth()
                            .padding(top = 25.dp)
                    ) {
                        if (viewModel.searchQuery.isBlank() && categories is Resource.Success) {
                            itemsIndexed(categories.data ?: emptyList()) { i, item ->
                                SuggestionBlock(
                                    suggestion = item.name,
                                    isSelected = item.url == viewModel.selectedCatalog?.url,
                                    onClick = { viewModel.onCatalogChange(item) },
                                    modifier = Modifier
                                        .ifElse(
                                            condition = i == 0,
                                            ifTrueModifier = focusRestorersModifiers.childModifier
                                        )
                                        .focusOnMount(itemKey = "category=${item.url}")
                                        .focusProperties {
                                            right = filtersGroupFocusRequester
                                        }
                                )
                            }
                        }
                        else if (viewModel.searchSuggestions.isNotEmpty()) {
                            itemsIndexed(viewModel.searchSuggestions) { i, suggestion ->
                                SuggestionBlock(
                                    suggestion = suggestion,
                                    onClick = { viewModel.onQueryChange(suggestion) },
                                    modifier = Modifier
                                        .ifElse(
                                            condition = i == 0,
                                            ifTrueModifier = focusRestorersModifiers.childModifier
                                        )
                                        .focusOnMount(itemKey = "suggestion=${suggestion}, query=${viewModel.searchQuery}")
                                        .focusProperties {
                                            right = filtersGroupFocusRequester
                                        }
                                )
                            }
                        }
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .focusGroup()
                        .weight(1F)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Icon(
                            painter = painterResource(id = UiCommonR.drawable.search_outlined),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(18.dp)
                        )

                        Text(
                            text = viewModel.searchQuery,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier
                                .weight(1F)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = createDefaultFocusRestorerModifier()
                            .focusRequester(filtersGroupFocusRequester)
                            .fillMaxWidth()
                    ) {
                        SearchFilter.entries.forEach { filter ->
                            FilterBlock(
                                modifier = Modifier
                                    .focusOnMount(itemKey = "filterButton=$filter"),
                                isSelected = viewModel.currentFilterSelected == filter,
                                name = stringResource(id = filter.resId),
                                onClick = {
                                    scope.launch {
                                        viewModel.onChangeFilter(filter)
                                        safeCall { listState.scrollToItem(0) }
                                    }
                                }
                            )
                        }
                    }

                    val filmSearchHeight = 215.dp
                    val filmSearchWidth = 165.dp
                    val filmsFocusRestorersModifiers = createInitialFocusRestorerModifiers()

                    TvLazyVerticalGrid(
                        columns = TvGridCells.Adaptive(150.dp),
                        modifier = filmsFocusRestorersModifiers.parentModifier,
                        pivotOffsets = PivotOffsets(0F, 0F),
                        state = listState,
                    ) {
                        itemsIndexed(viewModel.searchResults) { i, film ->
                            FilmCard(
                                modifier = Modifier
                                    .focusOnMount(itemKey = "filmIndex=$i")
                                    .ifElse(
                                        condition = i == 0,
                                        ifTrueModifier = filmsFocusRestorersModifiers.childModifier
                                    ),
                                film = film,
                                onClick = navigator::openFilmScreen,
                                filmCardHeight = filmSearchHeight,
                                filmCardWidth = filmSearchWidth,
                            )
                        }
                    }
                }
            }
        }
    }
}