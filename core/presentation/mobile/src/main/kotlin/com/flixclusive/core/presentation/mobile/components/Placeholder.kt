package com.flixclusive.core.presentation.mobile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Placeholder(
    modifier: Modifier = Modifier,
    tonalElevation: Dp = 3.dp,
) {
    Spacer(
        modifier = modifier
            .then(
                Modifier.background(
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(tonalElevation),
                    shape = MaterialTheme.shapes.extraSmall,
                ),
            ),
    )
}
