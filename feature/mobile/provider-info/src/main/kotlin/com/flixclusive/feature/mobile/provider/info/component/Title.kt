package com.flixclusive.feature.mobile.provider.info.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.flixclusive.feature.mobile.provider.info.LABEL_SIZE_IN_SP

@Composable
internal fun Title(
    modifier: Modifier = Modifier,
    text: String
) {
    Text(
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Black,
            fontSize = LABEL_SIZE_IN_SP
        ),
        modifier = modifier
    )
}