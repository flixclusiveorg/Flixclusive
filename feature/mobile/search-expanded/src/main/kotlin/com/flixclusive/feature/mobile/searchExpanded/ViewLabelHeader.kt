package com.flixclusive.feature.mobile.searchExpanded

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.asAdaptiveTextStyle

@Composable
internal fun ViewLabelHeader(
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 5.dp),
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Black,
            color = LocalContentColor.current.copy(0.8F),
            style = MaterialTheme.typography.labelMedium.asAdaptiveTextStyle(14.sp),
            modifier = Modifier.padding(horizontal = 10.dp),
        )
    }
}
