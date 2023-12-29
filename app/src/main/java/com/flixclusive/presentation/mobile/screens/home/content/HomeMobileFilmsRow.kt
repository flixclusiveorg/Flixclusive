package com.flixclusive.presentation.mobile.screens.home.content

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flixclusive.R
import com.flixclusive.domain.model.config.HomeCategoryItem
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.TMDBSearchItem
import com.flixclusive.presentation.common.PagingState
import com.flixclusive.presentation.common.viewmodels.home.PaginationStateInfo
import com.flixclusive.presentation.mobile.common.composables.film.FilmCard
import com.flixclusive.presentation.mobile.common.composables.film.FilmCardPlaceholder
import com.flixclusive.presentation.mobile.main.LABEL_START_PADDING
import com.flixclusive.presentation.mobile.utils.ComposeMobileUtils.colorOnMediumEmphasisMobile
import com.flixclusive.presentation.utils.LazyListUtils.shouldPaginate

@Composable
fun HomeMobileFilmsRow(
    modifier: Modifier = Modifier,
    categoryItem: HomeCategoryItem,
    paginationState: PaginationStateInfo,
    showCardTitle: Boolean,
    films: List<Film>,
    onFilmClick: (Film) -> Unit,
    onFilmLongClick: (Film) -> Unit,
    paginate: (query: String, page: Int) -> Unit,
    onSeeAllClick: () -> Unit
) {
    val listState = rememberLazyListState()

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

    if(films.isNotEmpty() || paginationState.canPaginate) {
        Column(
            modifier = modifier
                .padding(vertical = if(showCardTitle) 3.dp else 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clickable {
                        onSeeAllClick()
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10))
                        .padding(vertical = 10.dp)
                ) {
                    Text(
                        text = categoryItem.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .weight(1F)
                            .padding(start = LABEL_START_PADDING)
                    )

                    Box(
                        modifier = Modifier
                            .padding(end = LABEL_START_PADDING),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.right_arrow),
                            contentDescription = stringResource(id = R.string.see_all),
                            tint = colorOnMediumEmphasisMobile(),
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onSeeAllClick() }
                        )
                    }
                }
            }

            LazyRow(state = listState) {
                items(films) { film ->

                    // Necessary if we ever encounter a trending cast/person model
                    if(film is TMDBSearchItem.PersonTMDBSearchItem)
                        return@items

                    FilmCard(
                        modifier = Modifier
                            .width(135.dp),
                        shouldShowTitle = showCardTitle,
                        film = film,
                        onClick = onFilmClick,
                        onLongClick = { onFilmLongClick(film) }
                    )
                }

                if(
                    paginationState.pagingState == PagingState.LOADING ||
                    paginationState.pagingState == PagingState.PAGINATING ||
                    paginationState.pagingState == PagingState.ERROR ||
                    paginationState.pagingState == PagingState.PAGINATING_EXHAUST ||
                    films.isEmpty()
                ) {
                    items(5) {
                        FilmCardPlaceholder(
                            modifier = Modifier
                                .width(135.dp)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}