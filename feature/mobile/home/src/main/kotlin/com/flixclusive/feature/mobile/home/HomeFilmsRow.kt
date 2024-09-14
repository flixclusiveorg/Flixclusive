package com.flixclusive.feature.mobile.home

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
import androidx.compose.material3.LocalContentColor
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
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.mobile.component.film.FilmCard
import com.flixclusive.core.ui.mobile.component.film.FilmCardPlaceholder
import com.flixclusive.core.ui.mobile.util.shouldPaginate
import com.flixclusive.core.ui.common.util.PagingState
import com.flixclusive.domain.home.PaginationStateInfo
import com.flixclusive.model.film.Film
import com.flixclusive.model.provider.Catalog
import com.flixclusive.core.ui.mobile.R as UiMobileR
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun HomeFilmsRow(
    modifier: Modifier = Modifier,
    catalogItem: Catalog,
    paginationState: PaginationStateInfo,
    showCardTitle: Boolean,
    films: List<Film>,
    onFilmClick: (Film) -> Unit,
    onFilmLongClick: (Film) -> Unit,
    paginate: (page: Int) -> Unit,
    onSeeAllClick: () -> Unit
) {
    val listState = rememberLazyListState()

    val shouldStartPaginate by remember {
        derivedStateOf {
            listState.shouldPaginate()
        }
    }

    LaunchedEffect(shouldStartPaginate) {
        if (
            shouldStartPaginate && paginationState.canPaginate
            && (paginationState.pagingState == PagingState.IDLE
                    || paginationState.pagingState == PagingState.ERROR)
        ) {
            paginate(paginationState.currentPage)
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
                        text = catalogItem.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .weight(1F)
                            .padding(start = 15.dp)
                    )

                    Box(
                        modifier = Modifier
                            .padding(end = 15.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = UiMobileR.drawable.right_arrow),
                            contentDescription = stringResource(id = LocaleR.string.see_all),
                            tint = LocalContentColor.current.onMediumEmphasis(),
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onSeeAllClick() }
                        )
                    }
                }
            }

            LazyRow(state = listState) {
                items(films) { film ->
                    FilmCard(
                        modifier = Modifier
                            .width(135.dp),
                        isShowingTitle = showCardTitle,
                        film = film,
                        onClick = onFilmClick,
                        onLongClick = { onFilmLongClick(film) }
                    )
                }

                if(
                    paginationState.pagingState == PagingState.LOADING ||
                    paginationState.pagingState == PagingState.PAGINATING ||
                    paginationState.pagingState == PagingState.ERROR ||
                    films.isEmpty()
                ) {
                    items(5) {
                        FilmCardPlaceholder(
                            modifier = Modifier
                                .width(135.dp)
                                .fillMaxHeight()
                                .padding(3.dp)
                        )
                    }
                }
            }
        }
    }
}