package com.flixclusive.core.ui.mobile.component.film

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.model.film.Genre

@Composable
fun GenreButton(
    genre: Genre,
    onClick: (Genre) -> Unit,
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 5.dp)
            .background(Color.Transparent)
            .border(
                width = 1.dp,
                color = LocalContentColor.current.onMediumEmphasis(),
                shape = CircleShape
            )
            .clip(CircleShape)
            .clickable { onClick(genre) }
    ) {
        Text(
            text = genre.name,
            style = MaterialTheme.typography.labelMedium,
            color = LocalContentColor.current.onMediumEmphasis(),
            modifier = Modifier
                .padding(
                    horizontal = 15.dp,
                    vertical = 4.dp
                )
        )
    }
}