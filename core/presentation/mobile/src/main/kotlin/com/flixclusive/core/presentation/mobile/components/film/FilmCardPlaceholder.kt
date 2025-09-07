package com.flixclusive.core.presentation.mobile.components.film

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.common.components.FilmCover
import com.flixclusive.core.presentation.common.extensions.placeholderEffect
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.core.presentation.mobile.util.MobileUiUtil.getAdaptiveFilmCardWidth

@Composable
fun FilmCardPlaceholder(
    modifier: Modifier = Modifier,
    cardWidth: Dp = getAdaptiveFilmCardWidth(),
    titleHeight: Dp = getAdaptiveDp(10.dp),
    isShowingTitle: Boolean = false,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Spacer(
            modifier = Modifier
                .width(cardWidth)
                .aspectRatio(FilmCover.Poster.ratio)
                .placeholderEffect()
        )

        if (isShowingTitle) {
            Box(
                modifier = Modifier
                    .padding(
                        vertical = 8.dp,
                        horizontal = 3.dp
                    )
            ) {
                Spacer(
                    modifier = Modifier
                        .height(titleHeight)
                        .fillMaxWidth()
                        .placeholderEffect()
                )
            }
        }
    }
}
