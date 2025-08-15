package com.flixclusive.core.ui.tv.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import com.flixclusive.core.presentation.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.onMediumEmphasis

@Composable
fun CustomLinearProgressIndicator(
    modifier: Modifier = Modifier,
    progress: Float,
    color: Color,
    trackColor: Color = color.onMediumEmphasis(),
) {
    Box(
        modifier = Modifier
            .height(4.dp)
            .then(modifier)
            .width(240.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(trackColor)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .fillMaxHeight()
                .background(color)
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Preview
@Composable
private fun CustomLinearProgressIndicatorPreview() {
    FlixclusiveTheme(isTv = true) {
        CustomLinearProgressIndicator(
            progress = 0.8F,
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(0.4F),
            modifier = Modifier.clip(MaterialTheme.shapes.large)
        )
    }
}
