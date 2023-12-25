package com.flixclusive.presentation.tv.screens.search

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.flixclusive.R
import com.flixclusive.presentation.common.PagingState
import com.flixclusive.presentation.destinations.FilmTvScreenDestination
import com.flixclusive.presentation.tv.common.composables.FilmCardTv
import com.flixclusive.presentation.tv.common.TvRootNavGraph
import com.flixclusive.presentation.tv.utils.ModifierTvUtils.createInitialFocusRestorerModifiers
import com.flixclusive.presentation.utils.ModifierUtils.ifElse
import com.flixclusive.presentation.utils.LazyListUtils.shouldPaginate
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.delay

val FilmSearchHeight = 215.dp

@OptIn(ExperimentalFoundationApi::class)
@TvRootNavGraph
@Destination
@Composable
fun SearchTvScreen(navigator: DestinationsNavigator) {
    val viewModel: SearchTvScreenViewModel = hiltViewModel()
    val listState = rememberTvLazyGridState()

    val focusRestorerModifiers = createInitialFocusRestorerModifiers()
    var lastSearchedQuery by remember { mutableStateOf(viewModel.searchQuery) }

    val shouldStartPaginate by remember {
        derivedStateOf {
            viewModel.canPaginate && listState.shouldPaginate()
        }
    }

    LaunchedEffect(shouldStartPaginate) {
        if(shouldStartPaginate && viewModel.pagingState == PagingState.IDLE)
            viewModel.getSearchItems()
    }


    LaunchedEffect(viewModel.searchQuery, lastSearchedQuery) {
        val queryIsNotEmpty = viewModel.searchQuery.isNotEmpty()
        val userIsTypingNewQuery = viewModel.searchQuery != lastSearchedQuery

        if(queryIsNotEmpty && userIsTypingNewQuery) {
            delay(1500L)
            viewModel.onSearchClick()
        }

        lastSearchedQuery = viewModel.searchQuery
    }

    Row(
        modifier = Modifier
            .padding(top = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.30F)
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
            modifier = Modifier.focusGroup()
                .weight(1F)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.search_outlined),
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

            TvLazyVerticalGrid(
                columns = TvGridCells.Adaptive(125.dp),
                modifier = focusRestorerModifiers.parentModifier
            ) {
                itemsIndexed(
                    items = viewModel.searchResults,
                    key = { i, film -> film.id * i }
                ) { i, film ->
                    FilmCardTv(
                        modifier = Modifier
                            .ifElse(
                                condition = i == 0,
                                ifTrueModifier = focusRestorerModifiers.childModifier
                            ),
                        film = film,
                        onClick = {
                            navigator.navigate(
                                FilmTvScreenDestination(it),
                                onlyIfResumed = true
                            )
                        },
                        filmCardHeight = FilmSearchHeight
                    )
                }
            }
        }
    }
}