package com.flixclusive.core.ui.mobile.component

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
import com.flixclusive.core.ui.common.GradientCircularProgressIndicator
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveTextStyle
import com.flixclusive.core.ui.common.util.adaptive.TextStyleMode
import com.flixclusive.core.ui.common.util.adaptive.TypographyStyle
import com.flixclusive.core.locale.R as LocaleR

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
            style =
                getAdaptiveTextStyle(
                    style = TypographyStyle.Label,
                    mode = TextStyleMode.Normal,
                    increaseBy = 5.sp,
                ).copy(fontWeight = FontWeight.Medium),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
    }
}
