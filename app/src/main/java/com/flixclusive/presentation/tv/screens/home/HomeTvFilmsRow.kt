package com.flixclusive.presentation.tv.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.flixclusive.domain.model.config.HomeCategoryItem
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.TMDBSearchItem
import com.flixclusive.presentation.common.PagingState
import com.flixclusive.presentation.common.viewmodels.home.FocusPosition
import com.flixclusive.presentation.common.viewmodels.home.PaginationStateInfo
import com.flixclusive.presentation.tv.common.FilmPadding
import com.flixclusive.presentation.tv.common.FilmRowItem
import com.flixclusive.presentation.tv.main.InitialDrawerWidth
import com.flixclusive.presentation.tv.utils.ModifierTvUtils.LabelStartPadding
import com.flixclusive.presentation.tv.utils.ModifierTvUtils.createInitialFocusRestorerModifiers
import com.flixclusive.presentation.tv.utils.ModifierTvUtils.ifElse
import com.flixclusive.presentation.tv.utils.ModifierTvUtils.onFilmFocusChange
import com.flixclusive.presentation.utils.LazyListUtils.shouldPaginate

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeTvFilmsRow(
    modifier: Modifier = Modifier,
    categoryItem: HomeCategoryItem,
    paginationState: PaginationStateInfo,
    films: List<Film>,
    rowIndex: Int,
    lastFocusedItem: FocusPosition,
    anItemHasBeenFocused: Boolean,
    onFilmClick: (film: Film) -> Unit,
    onFocusedFilmChange: (film: Film, index: Int) -> Unit,
    paginate: (query: String, page: Int) -> Unit,
) {
    val listState = rememberTvLazyListState()

    val initialFocusPosition = remember { listState.firstVisibleItemIndex }

    val shouldStartPaginate by remember {
        derivedStateOf {
            listState.shouldPaginate()
        }
    }

    LaunchedEffect(shouldStartPaginate) {
        if (shouldStartPaginate && paginationState.canPaginate && paginationState.pagingState == PagingState.IDLE) {
            paginate(
                categoryItem.query,
                paginationState.currentPage
            )
        }
    }

    fun restoreFocus(focusRequester: FocusRequester, columnIndex: Int) {
        val shouldFocusThisItem = lastFocusedItem.row == rowIndex
                && lastFocusedItem.column == columnIndex
                && !anItemHasBeenFocused

        if (shouldFocusThisItem) {
            focusRequester.requestFocus()
        }
    }

    if (films.isNotEmpty()) {
        val focusRequesterModifiers = createInitialFocusRestorerModifiers()

        LaunchedEffect(lastFocusedItem) {
            if(lastFocusedItem.column == 0 && !paginationState.canPaginate)
                listState.scrollToItem(0)
        }

        Column(
            modifier = modifier
                .focusGroup()
        ) {
            Text(
                text = categoryItem.name,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                ),
                modifier = Modifier
                    .padding(start = LabelStartPadding.start + InitialDrawerWidth)
                    .padding(
                        bottom = FilmPadding,
                        top = 18.dp
                    )
            )

            TvLazyRow(
                modifier = focusRequesterModifiers.parentModifier,
                pivotOffsets = PivotOffsets(parentFraction = 0.07F),
                state = listState,
                contentPadding = PaddingValues(start = LabelStartPadding.start + InitialDrawerWidth)
            ) {
                items(
                    count = if(paginationState.canPaginate) films.size else Int.MAX_VALUE,
                    key = {
                        val index = it % films.size
                        films[index].id
                    }
                ) {
                    val columnIndex = it % films.size
                    val film = films[columnIndex]

                    // Necessary if we ever encounter a trending cast/person model
                    if(film is TMDBSearchItem.PersonTMDBSearchItem)
                        return@items

                    val focusRequester = remember { FocusRequester() }

                    FilmRowItem(
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .ifElse(
                                condition = columnIndex == initialFocusPosition,
                                ifTrueModifier = focusRequesterModifiers.childModifier
                            )
                            .onPlaced {
                                restoreFocus(focusRequester, columnIndex)
                            }
                            .onFilmFocusChange {
                                onFocusedFilmChange(film, columnIndex)
                            },
                        film = film,
                        onClick = onFilmClick
                    )
                }
            }
        }
    }
}