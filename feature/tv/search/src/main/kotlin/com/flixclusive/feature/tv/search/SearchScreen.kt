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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.itemsIndexed
import androidx.tv.foundation.lazy.grid.rememberTvLazyGridState
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.flixclusive.core.ui.common.navigation.CommonScreenNavigator
import com.flixclusive.core.ui.common.util.ifElse
import com.flixclusive.core.ui.tv.component.FilmCard
import com.flixclusive.core.ui.tv.util.LocalFocusTransferredOnLaunchProvider
import com.flixclusive.core.ui.tv.util.createInitialFocusRestorerModifiers
import com.flixclusive.core.ui.tv.util.focusOnMount
import com.flixclusive.core.ui.tv.util.shouldPaginate
import com.flixclusive.core.ui.tv.util.useLocalCurrentRoute
import com.flixclusive.core.ui.tv.util.useLocalLastFocusedItemPerDestination
import com.flixclusive.core.util.common.ui.PagingState
import com.flixclusive.feature.tv.search.component.KEYBOARD_FOCUS_KEY_FORMAT
import com.flixclusive.feature.tv.search.component.SearchCustomKeyboard
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.delay
import com.flixclusive.core.ui.common.R as UiCommonR

@OptIn(ExperimentalTvMaterial3Api::class)
@Destination
@Composable
fun SearchScreen(
    navigator: CommonScreenNavigator
) {
    val viewModel: SearchScreenViewModel = hiltViewModel()
    val listState = rememberTvLazyGridState()

    val focusRestorerModifiers = createInitialFocusRestorerModifiers()
    var lastSearchedQuery by remember { mutableStateOf(viewModel.searchQuery) }

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
            viewModel.onSearch()
        }

        lastSearchedQuery = viewModel.searchQuery
    }

    val lastFocusedItems = useLocalLastFocusedItemPerDestination()
    val currentRoute = useLocalCurrentRoute()
    LaunchedEffect(Unit) {
        lastFocusedItems.getOrPut(currentRoute) {
            String.format(KEYBOARD_FOCUS_KEY_FORMAT, "a")
        }
    }

    LocalFocusTransferredOnLaunchProvider {
        Row(
            modifier = Modifier
                .padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .focusGroup()
                    .fillMaxWidth(0.35F)
                    .fillMaxHeight()
            ) {
                SearchCustomKeyboard(
                    currentSearchQuery = viewModel.searchQuery,
                    onKeyboardClick = {
                        val newQuery = viewModel.searchQuery + it
                        viewModel.onQueryChange(query = newQuery)
                    },
                    onBackspacePress = {
                        viewModel.onQueryChange(query = viewModel.searchQuery.dropLast(1))
                    }
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .focusGroup()
                    .weight(1F)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                        modifier = Modifier
                            .weight(1F)
                    )
                }

                val filmSearchHeight = 215.dp
                val filmSearchWidth = 165.dp

                TvLazyVerticalGrid(
                    columns = TvGridCells.Adaptive(150.dp),
                    modifier = focusRestorerModifiers.parentModifier,
                    state = listState,
                ) {
                    itemsIndexed(viewModel.searchResults) { i, film ->
                        FilmCard(
                            modifier = Modifier
                                .ifElse(
                                    condition = i == 0,
                                    ifTrueModifier = focusRestorerModifiers.childModifier
                                )
                                .focusOnMount(itemKey = "filmIndex=$i"),
                            film = film,
                            onClick = {
                                navigator.openFilmScreen(it)
                            },
                            filmCardHeight = filmSearchHeight,
                            filmCardWidth = filmSearchWidth,
                        )
                    }
                }
            }
        }
    }
}