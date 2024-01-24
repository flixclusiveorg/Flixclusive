package com.flixclusive.feature.tv.film.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.NonInteractiveSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flixclusive.core.ui.common.util.fadingEdge
import com.flixclusive.core.ui.common.util.ifElse
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.tv.component.FilmCard
import com.flixclusive.core.ui.tv.component.FilmCardShape
import com.flixclusive.core.ui.tv.component.NonFocusableSpacer
import com.flixclusive.core.ui.tv.util.FocusPosition
import com.flixclusive.core.ui.tv.util.createInitialFocusRestorerModifiers
import com.flixclusive.core.ui.tv.util.useLocalDrawerWidth
import com.flixclusive.core.util.common.ui.UiText
import com.flixclusive.model.tmdb.Film

@OptIn(ExperimentalComposeUiApi::class, ExperimentalTvMaterial3Api::class)
@Composable
internal fun FilmsRow(
    modifier: Modifier = Modifier,
    currentFilm: Film,
    label: UiText,
    rowIndex: Int,
    @DrawableRes iconId: Int,
    films: List<Film>,
    hasFocus: Boolean,
    lastFocusedItem: FocusPosition?,
    anItemHasBeenClicked: Boolean,
    onFilmClick: (Int, Film) -> Unit,
    onFocusChange: (Boolean) -> Unit
) {
    val listState = rememberTvLazyListState()
    val initialFocusPosition = remember { listState.firstVisibleItemIndex }

    val focusRestorerModifiers = createInitialFocusRestorerModifiers()
    val leftFade = Brush.horizontalGradient(
        0F to Color.Transparent,
        0.05F to Color.Red
    )

    fun restoreFocus(focusRequester: FocusRequester, columnIndex: Int) {
        val shouldFocusThisItem = lastFocusedItem?.row == rowIndex
                && lastFocusedItem.column == columnIndex
                && !anItemHasBeenClicked

        if (shouldFocusThisItem) {
            focusRequester.requestFocus()
        }
    }

    Surface(
        shape = RectangleShape,
        colors = NonInteractiveSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = if(hasFocus) Color.White else LocalContentColor.current.onMediumEmphasis()
        )
    ) {
        Column(
            modifier = modifier.focusGroup(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(start = useLocalDrawerWidth())
            ) {
                Icon(
                    painter = painterResource(id = iconId),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )

                Text(
                    text = label.asString(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Box {
                TvLazyRow(
                    modifier = focusRestorerModifiers.parentModifier
                        .fadingEdge(leftFade)
                        .onFocusChanged {
                            onFocusChange(it.hasFocus)
                        },
                    state = listState,
                    contentPadding = PaddingValues(start = useLocalDrawerWidth()),
                    pivotOffsets = PivotOffsets(parentFraction = 0.05F)
                ) {
                    itemsIndexed(films) { columnIndex, film ->
                        val focusRequester = remember { FocusRequester() }

                        Box {
                            FilmCard(
                                modifier = Modifier
                                    .focusRequester(focusRequester)
                                    .ifElse(
                                        condition = columnIndex == initialFocusPosition,
                                        ifTrueModifier = focusRestorerModifiers.childModifier
                                    )
                                    .onPlaced {
                                        restoreFocus(focusRequester, columnIndex)
                                    }
                                    .focusProperties {
                                        if (columnIndex == films.lastIndex) {
                                            right = FocusRequester.Cancel
                                        }
                                    },
                                film = film,
                                onClick = {
                                    if (currentFilm.id != it.id) {
                                        onFilmClick(columnIndex, it)
                                    }
                                }
                            )

                            Spacer(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        color = if (!hasFocus) {
                                            MaterialTheme.colorScheme.surface.onMediumEmphasis()
                                        } else Color.Transparent,
                                        shape = FilmCardShape
                                    )
                            )
                        }
                    }

                    items(10) {
                        NonFocusableSpacer(width = 80.dp)
                    }
                }
            }
        }
    }
}