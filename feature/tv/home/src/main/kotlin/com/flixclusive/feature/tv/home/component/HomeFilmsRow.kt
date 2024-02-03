package com.flixclusive.feature.tv.home.component

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.flixclusive.core.ui.common.util.ifElse
import com.flixclusive.core.ui.tv.component.FilmCard
import com.flixclusive.core.ui.tv.component.FilmCardHeight
import com.flixclusive.core.ui.tv.component.FilmPadding
import com.flixclusive.core.ui.tv.util.LabelStartPadding
import com.flixclusive.core.ui.tv.util.createInitialFocusRestorerModifiers
import com.flixclusive.core.ui.tv.util.focusOnMount
import com.flixclusive.core.ui.tv.util.shouldPaginate
import com.flixclusive.core.ui.tv.util.useLocalDrawerWidth
import com.flixclusive.core.util.common.ui.PagingState
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.domain.home.PaginationStateInfo
import com.flixclusive.model.configuration.HomeCategoryItem
import com.flixclusive.model.tmdb.Film

internal const val HOME_FOCUS_KEY_FORMAT = "row=%d, column=%d"

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun HomeFilmsRow(
    modifier: Modifier = Modifier,
    categoryItem: HomeCategoryItem,
    paginationState: PaginationStateInfo,
    films: List<Film>,
    rowIndex: Int,
    onFilmClick: (film: Film) -> Unit,
    onFocusedFilmChange: (film: Film) -> Unit,
    paginate: (query: String, page: Int) -> Unit,
) {
    val focusRestorers = createInitialFocusRestorerModifiers()

    val listState = rememberTvLazyListState()

    val shouldStartPaginate by remember {
        derivedStateOf {
            listState.shouldPaginate()
        }
    }

    LaunchedEffect(listState.firstVisibleItemIndex) {
        safeCall {
            if (
                films.isNotEmpty()
                && listState.firstVisibleItemIndex % films.size == 1
                && listState.firstVisibleItemIndex > films.size
                && !paginationState.canPaginate
            ) {
                listState.scrollToItem(0)
            }
        }
    }

    LaunchedEffect(shouldStartPaginate) {
        if (
            shouldStartPaginate && paginationState.canPaginate
            && (paginationState.pagingState == PagingState.IDLE
            || paginationState.pagingState == PagingState.ERROR
            || films.isEmpty())
        ) {
            paginate(
                categoryItem.query,
                paginationState.currentPage
            )
        }
    }

    Column(
        modifier = Modifier
            .heightIn(min = FilmPadding.bottom + 18.dp + FilmCardHeight)
    ) {
        Text(
            text = categoryItem.name,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            ),
            modifier = Modifier
                .padding(start = LabelStartPadding.start + useLocalDrawerWidth())
                .padding(
                    bottom = FilmPadding.bottom,
                    top = 18.dp
                )
        )

        TvLazyRow(
            modifier = Modifier
                .focusGroup()
                .then(focusRestorers.parentModifier),
            pivotOffsets = PivotOffsets(parentFraction = 0.07F),
            state = listState,
            contentPadding = PaddingValues(
                start = LabelStartPadding.start + useLocalDrawerWidth()
            )
        ) {
            items(
                count = if (paginationState.canPaginate) films.size else Int.MAX_VALUE
            ) {
                val columnIndex = it % films.size
                val film = films[columnIndex]

                val key = String.format(HOME_FOCUS_KEY_FORMAT, rowIndex, columnIndex)

                FilmCard(
                    modifier = Modifier
                        .ifElse(
                            condition = it == 0,
                            ifTrueModifier = focusRestorers.childModifier
                        )
                        .focusOnMount(
                            itemKey = key,
                            onFocus = {
                                onFocusedFilmChange(film)
                            }
                        ),
                    film = film,
                    onClick = onFilmClick
                )
            }
        }

    }
}