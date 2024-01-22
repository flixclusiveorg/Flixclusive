package com.flixclusive.core.ui.mobile.component.film

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
import com.flixclusive.core.ui.common.util.placeholderEffect

@Composable
fun FilmCardPlaceholder(
    modifier: Modifier = Modifier,
    shouldShowTitle: Boolean = false,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
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