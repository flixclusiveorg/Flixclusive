package com.flixclusive.core.ui.mobile.component.film

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.FilmCover
import com.flixclusive.core.ui.common.util.DummyDataForPreview.getDummyFilm
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.model.film.Film

@Composable
fun FilmCard(
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier.fillMaxSize(),
    film: Film,
    isShowingTitle: Boolean = false,
    onClick: (Film) -> Unit,
    onLongClick: (Film) -> Unit,
) {
    var isHidingPlaceholder by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(3.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
        ) {
            FilmCover.Poster(
                imagePath = film.posterImage,
                imageSize = "w300",
                showPlaceholder = false,
                onSuccess = { isHidingPlaceholder = true },
                onClick = { onClick(film) },
                onLongClick = { onLongClick(film) },
            )

            this@Column.AnimatedVisibility(
                visible = !isHidingPlaceholder,
                enter = fadeIn(),
                exit = fadeOut(),
                label = ""
            ) {
                FilmCardPlaceholder(
                    isShowingTitle = isShowingTitle,
                    title = film.title,
                    modifier = Modifier
                        .aspectRatio(FilmCover.Poster.ratio)
                )
            }
        }

        if(isShowingTitle) {
            Text(
                text = film.title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 12.sp
                ),
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                color = LocalContentColor.current.onMediumEmphasis(emphasis = 0.8F),
                maxLines = 1,
                modifier = Modifier
                    .padding(vertical = 5.dp)
            )
        }
    }
}

@Preview
@Composable
private fun FilmCardPreview() {
    FlixclusiveTheme {
        Surface {
            Row {
                FilmCard(
                    modifier = Modifier.weight(1F),
                    film = getDummyFilm(),
                    onClick = {},
                    onLongClick = {}
                )
            }
        }
    }
}