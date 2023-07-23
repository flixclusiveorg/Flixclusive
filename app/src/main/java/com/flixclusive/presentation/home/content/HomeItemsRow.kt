package com.flixclusive.presentation.home.content

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flixclusive.R
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.presentation.common.UiText
import com.flixclusive.presentation.film.FilmCard
import com.flixclusive.presentation.main.LABEL_START_PADDING

@Composable
fun HomeItemsRow(
    modifier: Modifier = Modifier,
    flag: String?,
    label: UiText,
    dataListProvider: () -> List<Film>? = { listOf() },
    onFilmClick: (Film) -> Unit,
    onFilmLongClick: (Film) -> Unit,
    onSeeAllClick: (String, String) -> Unit
) {
    val context = LocalContext.current

    if(dataListProvider()?.isNotEmpty() == true) {
        Column(
            modifier = modifier
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(bottom = 5.dp)
                    .graphicsLayer {
                        shape = RoundedCornerShape(10)
                        clip = true
                    }
                    .clickable(enabled = flag != null) {
                        flag?.let {
                            onSeeAllClick(it, label.asString(context))
                        }
                    }
            ) {
                Text(
                    text = label.asString(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .weight(1F)
                        .padding(start = LABEL_START_PADDING)
                )

                if(flag != null) {
                    Text(
                        text = UiText.StringResource(R.string.see_all).asString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(end = LABEL_START_PADDING)
                            .clickable { onSeeAllClick(flag, label.asString(context)) }
                    )
                }
            }

            LazyRow {
                items(dataListProvider()!!, key = { it.id }) { film ->
                    FilmCard(
                        modifier = Modifier
                            .width(135.dp),
                        shouldShowTitle = false,
                        film = film,
                        onClick = onFilmClick,
                        onLongClick = { onFilmLongClick(film) }
                    )
                }
            }
        }
    }
}