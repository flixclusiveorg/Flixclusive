package com.flixclusive.core.presentation.mobile.components.media

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.common.components.MediaCover
import com.flixclusive.core.presentation.mobile.components.Placeholder
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp

@Composable
fun MediaCardPlaceholder(
    modifier: Modifier = Modifier,
    titleHeight: Dp = getAdaptiveDp(10.dp),
    isShowingTitle: Boolean = false,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Placeholder(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(MediaCover.Poster.ratio)
        )

        if (isShowingTitle) {
            Box(
                modifier = Modifier
                    .padding(
                        vertical = 8.dp,
                        horizontal = 3.dp
                    )
            ) {
                Placeholder(
                    modifier = Modifier
                        .height(titleHeight)
                        .fillMaxWidth()
                )
            }
        }
    }
}
