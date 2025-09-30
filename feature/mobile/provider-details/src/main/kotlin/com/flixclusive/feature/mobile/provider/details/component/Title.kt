package com.flixclusive.feature.mobile.provider.details.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.feature.mobile.provider.details.util.ProviderDetailsUiCommon.LABEL_SIZE_IN_SP

@Composable
internal fun Title(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        fontWeight = FontWeight.Black,
        style = MaterialTheme.typography.titleMedium.asAdaptiveTextStyle(LABEL_SIZE_IN_SP),
        modifier = modifier
    )
}
