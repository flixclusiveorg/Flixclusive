package com.flixclusive.feature.mobile.user.pin.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.asAdaptiveTextStyle

@Composable
internal fun HeaderLabel(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier,
        text = title,
        fontWeight = FontWeight.Black,
        style = MaterialTheme.typography.headlineSmall.asAdaptiveTextStyle(),
    )
}
