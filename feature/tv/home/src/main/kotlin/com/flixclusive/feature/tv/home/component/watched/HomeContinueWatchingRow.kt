package com.flixclusive.feature.tv.home.component.watched

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.flixclusive.core.ui.tv.component.FilmCardHeight
import com.flixclusive.core.ui.tv.component.FilmPadding
import com.flixclusive.core.ui.tv.util.LabelStartPadding
import com.flixclusive.core.ui.tv.util.focusOnMount
import com.flixclusive.core.ui.tv.util.getLocalDrawerWidth
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.model.database.WatchHistoryItem
import com.flixclusive.model.film.Film
import kotlinx.coroutines.launch
import com.flixclusive.core.locale.R as LocaleR

internal const val HOME_WATCHED_FILMS_FOCUS_KEY_FORMAT = "watchedRow=%d, watchedColumn=%d"

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun HomeContinueWatchingRow(
    modifier: Modifier = Modifier,
    items: List<WatchHistoryItem>,
    onPlayClick: (Film) -> Unit,
) {
    val listState = rememberTvLazyListState()
    val scope = rememberCoroutineScope()

    BackHandler(enabled = listState.firstVisibleItemIndex > 0) {
        scope.launch {
            safeCall {
                listState.animateScrollToItem(0)
            }
        }
    }

    Column(
        modifier = modifier
            .heightIn(min = FilmPadding.bottom + 18.dp + FilmCardHeight)
    ) {
        Text(
            text = stringResource(id = LocaleR.string.continue_watching),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Black,
                fontSize = 40.sp
            ),
            modifier = Modifier
                .padding(start = LabelStartPadding.start + getLocalDrawerWidth())
                .padding(
                    bottom = FilmPadding.bottom,
                    top = 18.dp
                )
        )

        TvLazyRow(
            modifier = Modifier.focusGroup(),
            state = listState,
            pivotOffsets = PivotOffsets(parentFraction = 0.07F),
            contentPadding = PaddingValues(
                start = LabelStartPadding.start + getLocalDrawerWidth()
            )
        ) {
            itemsIndexed(items = items) { i, item ->
                val key = String.format(HOME_WATCHED_FILMS_FOCUS_KEY_FORMAT, 0, i)

                WatchedFilmCard(
                    modifier = Modifier
                        .focusOnMount(itemKey = key),
                    watchHistoryItem = item,
                    onClick = { onPlayClick(item.film) },
                )
            }
        }

    }
}