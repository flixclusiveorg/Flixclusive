package com.flixclusive.core.presentation.mobile.components.film

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.drawables.R
import com.flixclusive.core.presentation.common.components.FilmCover
import com.flixclusive.core.presentation.common.util.DummyDataForPreview.getFilm
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.Companion.getAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.model.film.Film

/**
 * A card component that displays a film's poster and optionally its title
 * if user prefers to see it.
 *
 * @param film The film to be displayed.
 * @param onClick A lambda function to be invoked when the card is clicked.
 * @param onLongClick A lambda function to be invoked when the card is long-clicked.
 * @param modifier An optional [Modifier] for this component.
 * @param isShowingTitle A boolean indicating whether to show the film's title below the poster
 * */
@Composable
fun FilmCard(
    film: Film,
    onClick: (Film) -> Unit,
    onLongClick: (Film) -> Unit,
    modifier: Modifier = Modifier,
    isShowingTitle: Boolean = false,
) {
    var showPlaceholder by rememberSaveable { mutableStateOf(true) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(3.dp).then(modifier),
    ) {
        Box(
            contentAlignment = Alignment.Center,
        ) {
            FilmCover.Poster(
                imagePath = film.posterImage,
                imageSize = "w300",
                title = film.title,
                onSuccess = { showPlaceholder = false },
                onClick = { onClick(film) },
                onLongClick = { onLongClick(film) },
                modifier = Modifier.background(
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                ),
            )

            if (showPlaceholder) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.matchParentSize(),
                ) {
                    Box(
                        contentAlignment = Alignment.BottomCenter,
                        modifier = Modifier.weight(0.4f),
                    ) {
                        AdaptiveIcon(
                            painter = painterResource(id = R.drawable.movie_icon),
                            contentDescription = film.title,
                            tint = LocalContentColor.current.copy(0.6f),
                            dp = 40.dp,
                            increaseBy = 10.dp,
                        )
                    }

                    Box(
                        contentAlignment = Alignment.TopCenter,
                        modifier = Modifier.weight(0.6F),
                    ) {
                        Text(
                            text = film.title,
                            style = getAdaptiveTextStyle(size = 12.sp),
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis,
                            color = LocalContentColor.current.copy(0.6f),
                            modifier = Modifier
                                .padding(8.dp),
                        )
                    }
                }
            }
        }

        if (isShowingTitle) {
            Text(
                text = film.title,
                style = getAdaptiveTextStyle(size = 12.sp),
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                color = LocalContentColor.current.copy(alpha = 0.8F),
                maxLines = 1,
                modifier = Modifier.padding(vertical = 5.dp),
            )
        }
    }
}

@Preview
@Composable
private fun FilmCardPreview() {
    FlixclusiveTheme {
        Surface {
            LazyRow {
                items(20) {
                    FilmCard(
                        film = getFilm(),
                        onClick = {},
                        onLongClick = {},
                    )
                }
            }
        }
    }
}
