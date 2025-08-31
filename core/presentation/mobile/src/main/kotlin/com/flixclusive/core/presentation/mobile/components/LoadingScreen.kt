package com.flixclusive.core.presentation.mobile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.common.components.GradientCircularProgressIndicator
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.Companion.getAdaptiveTextStyle
import com.flixclusive.core.strings.R as LocaleR

@Composable
fun LoadingScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(25.dp, Alignment.CenterVertically),
    ) {
        GradientCircularProgressIndicator(
            size = 40.dp,
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.tertiary,
            ),
        )

        Text(
            text = stringResource(LocaleR.string.loading),
            style = getAdaptiveTextStyle(
                style = AdaptiveTextStyle.Normal(),
                increaseBy = 5.sp,
            ).copy(fontWeight = FontWeight.Medium),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
    }
}
