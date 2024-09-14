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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
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
import com.flixclusive.core.ui.tv.util.LabelStartPadding
import com.flixclusive.core.ui.tv.util.createInitialFocusRestorerModifiers
import com.flixclusive.core.ui.tv.util.getLocalDrawerWidth
import com.flixclusive.core.ui.tv.util.hasPressedLeft
import com.flixclusive.core.locale.UiText
import com.flixclusive.model.film.Film

@OptIn(ExperimentalComposeUiApi::class, ExperimentalTvMaterial3Api::class)
@Composable
internal fun FilmsRow(
    modifier: Modifier = Modifier,
    currentFilm: Film,
    label: UiText,
    @DrawableRes iconId: Int,
    films: List<Film>,
    hasFocus: Boolean,
    onFilmClick: (Film) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    goBack: () -> Unit,
) {
    val listState = rememberTvLazyListState()
    val leftFade = Brush.horizontalGradient(
        0F to Color.Transparent,
        0.05F to Color.Red
    )

    var isFirstItemFullyFocused by remember { mutableStateOf(true) }

    Surface(
        shape = RectangleShape,
        colors = NonInteractiveSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = if(hasFocus) Color.White else LocalContentColor.current.onMediumEmphasis()
        ),
    ) {
        Column(
            modifier = modifier.focusGroup(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(start = LabelStartPadding.start + getLocalDrawerWidth())
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

            val focusRestorers = createInitialFocusRestorerModifiers()
            val firstInitialIndex = remember { listState.firstVisibleItemIndex }

            Box {
                TvLazyRow(
                    modifier = focusRestorers.parentModifier
                        .fadingEdge(leftFade)
                        .onFocusChanged {
                            onFocusChange(it.hasFocus)
                        },
                    state = listState,
                    contentPadding = PaddingValues(start = LabelStartPadding.start + getLocalDrawerWidth()),
                    pivotOffsets = PivotOffsets(parentFraction = 0.05F)
                ) {
                    itemsIndexed(films) { columnIndex, film ->
                        Box {
                            FilmCard(
                                modifier = Modifier
                                    .ifElse(
                                        condition = columnIndex == firstInitialIndex,
                                        ifTrueModifier = focusRestorers.childModifier
                                    )
                                    .ifElse(
                                        condition = columnIndex == firstInitialIndex,
                                        ifTrueModifier = Modifier.onKeyEvent {
                                            if (hasPressedLeft(it) && isFirstItemFullyFocused) {
                                                goBack()
                                                return@onKeyEvent true
                                            } else isFirstItemFullyFocused = true

                                            false
                                        },
                                        ifFalseModifier = Modifier.onPreviewKeyEvent {
                                            isFirstItemFullyFocused = false
                                            false
                                        }
                                    )
                                    .focusProperties {
                                        if (columnIndex == films.lastIndex) {
                                            right = FocusRequester.Cancel
                                        }
                                    },
                                film = film,
                                onClick = {
                                    if (currentFilm.identifier != it.identifier) {
                                        onFilmClick(it)
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