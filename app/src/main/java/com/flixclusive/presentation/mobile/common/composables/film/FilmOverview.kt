package com.flixclusive.presentation.mobile.common.composables.film

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.flixclusive.presentation.mobile.utils.ComposeMobileUtils.colorOnMediumEmphasisMobile

@Composable
fun FilmOverview(
    overview: String?,
    modifier: Modifier = Modifier
) {
    Text(
        text = overview ?: "",
        style = MaterialTheme.typography.bodySmall,
        color = colorOnMediumEmphasisMobile(MaterialTheme.colorScheme.onSurface),
        modifier = modifier
    )
}