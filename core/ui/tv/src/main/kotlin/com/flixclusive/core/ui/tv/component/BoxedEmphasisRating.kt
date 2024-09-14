package com.flixclusive.core.ui.tv.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.flixclusive.core.ui.common.util.formatRating

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun BoxedEmphasisRating(
    rating: Double?,
    containerColor: Color = MaterialTheme.colorScheme.tertiary,
    contentColor: Color = MaterialTheme.colorScheme.onTertiary,
    boxShape: Shape = FilmCardShape
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(containerColor, boxShape)
            .widthIn(min = 25.dp)
    ) {
        Text(
            text = formatRating(rating).asString(),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            ),
            color = contentColor,
            modifier = Modifier
                .padding(3.dp)
        )
    }
}