package com.flixclusive.feature.mobile.user.pin.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveTextStyle
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveTextStyle
import com.flixclusive.core.ui.common.util.adaptive.TypographyStyle

@Composable
internal fun HeaderLabel(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier,
        text = title,
        style =
            getAdaptiveTextStyle(
                style = TypographyStyle.Title,
                style = AdaptiveTextStyle.Emphasized,
                size = 25.sp,
            ),
    )
}
