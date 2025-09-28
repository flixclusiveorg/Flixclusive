package com.flixclusive.core.presentation.mobile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.flixclusive.core.presentation.common.theme.Elevations
import com.flixclusive.core.presentation.mobile.theme.MobileColors.surfaceColorAtElevation

@Composable
fun Placeholder(
    modifier: Modifier = Modifier,
    elevation: Float = Elevations.LEVEL_1,
) {
    Spacer(
        modifier = modifier
            .then(
                Modifier.background(
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(elevation),
                    shape = MaterialTheme.shapes.small,
                ),
            ),
    )
}

@Composable
fun Placeholder(
    modifier: Modifier = Modifier,
    tonalElevation: Float = Elevations.LEVEL_1,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .then(
                Modifier.background(
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(tonalElevation),
                    shape = MaterialTheme.shapes.small,
                ),
            ),
    ) {
        content()
    }
}
