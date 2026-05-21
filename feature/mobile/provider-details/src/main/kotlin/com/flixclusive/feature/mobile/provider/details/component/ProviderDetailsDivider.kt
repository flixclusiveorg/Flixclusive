package com.flixclusive.feature.mobile.provider.details.component

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun ProviderDetailsDivider(
    modifier: Modifier = Modifier
) {
    HorizontalDivider(
        thickness = 0.5.dp,
        color = LocalContentColor.current.copy(alpha = 0.3f),
        modifier = modifier
    )
}
