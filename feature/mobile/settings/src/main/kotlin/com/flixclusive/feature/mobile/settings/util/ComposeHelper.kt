package com.flixclusive.feature.mobile.settings.util

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle

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
                    width = 3F,
                    join = StrokeJoin.Round
                ),
                background = Color.Transparent
            )
        )
    }
}