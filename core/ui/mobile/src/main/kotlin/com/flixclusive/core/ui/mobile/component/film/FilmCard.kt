package com.flixclusive.core.ui.mobile.component.film

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.ui.common.FilmCover
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.model.tmdb.Film

@Composable
fun FilmCard(
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier.fillMaxSize(),
    shouldShowTitle: Boolean = false,
    film: Film,
    onClick: (Film) -> Unit,
    onLongClick: (Film) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(3.dp)
    ) {
        FilmCover.Poster(
            imagePath = film.posterImage,
            imageSize = "w300",
            onClick = { onClick(film) },
            onLongClick = { onLongClick(film) },
        )

        if(shouldShowTitle) {
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