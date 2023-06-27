package com.flixclusive.presentation.film

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.flixclusive.ui.theme.colorOnMediumEmphasis

@Composable
fun FilmOverview(
    overview: String?,
    modifier: Modifier = Modifier
) {
    Text(
        text = overview ?: "",
        style = MaterialTheme.typography.bodySmall,
        color = colorOnMediumEmphasis(MaterialTheme.colorScheme.onSurface),
        modifier = modifier
    )
}