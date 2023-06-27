package com.flixclusive.presentation.film

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flixclusive.presentation.common.composables.placeholderEffect

@Composable
fun FilmCardPlaceholder(
    modifier: Modifier = Modifier,
    shouldShowTitle: Boolean = true,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(horizontal = 3.dp)
    ) {
        Spacer(
            modifier = Modifier
                .height(200.dp)
                .fillMaxWidth()
                .padding(3.dp)
                .placeholderEffect()
        )

        if(shouldShowTitle) {
            Box(
                modifier = Modifier
                    .padding(
                        vertical = 8.dp,
                        horizontal = 3.dp
                    )
            ) {
                Spacer(
                    modifier = Modifier
                        .height(10.dp)
                        .fillMaxWidth()
                        .placeholderEffect()
                )
            }
        }
    }
}