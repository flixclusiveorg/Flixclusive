package com.flixclusive.core.presentation.mobile.components.film

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.common.components.FilmCover
import com.flixclusive.core.presentation.common.util.DummyDataForPreview.getDummyFilm
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.Companion.getAdaptiveTextStyle
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(3.dp).then(modifier)
    ) {
        FilmCover.Poster(
            imagePath = film.posterImage,
            imageSize = "w300",
            title = film.title,
            onClick = { onClick(film) },
            onLongClick = { onLongClick(film) },
        )

        if(isShowingTitle) {
            Text(
                text = film.title,
                style = getAdaptiveTextStyle(size = 12.sp),
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                color = LocalContentColor.current.copy(alpha = 0.8F),
                maxLines = 1,
                modifier = Modifier.padding(vertical = 5.dp)
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
                        film = getDummyFilm(),
                        onClick = {},
                        onLongClick = {}
                    )
                }
            }
        }
    }
}
