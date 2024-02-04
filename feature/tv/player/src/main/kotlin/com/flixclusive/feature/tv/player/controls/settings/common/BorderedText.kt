package com.flixclusive.feature.tv.player.controls.settings.common

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun BorderedText(
    modifier: Modifier = Modifier,
    text: String,
    borderColor: Color,
    style: TextStyle
) {
    Box(
        modifier = modifier
    ) {
        Text(
            text = text,
            style = style
        )

        Text(
            text = text,
            color = borderColor,
            style = style.copy(
                drawStyle = Stroke(
                    miter = 10F,
                    width = 2F,
                    cap = StrokeCap.Square,
                    join = StrokeJoin.Miter
                ),
                background = Color.Transparent
            )
        )
    }
}