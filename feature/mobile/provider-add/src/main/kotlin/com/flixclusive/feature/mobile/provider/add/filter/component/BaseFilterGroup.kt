package com.flixclusive.feature.mobile.provider.add.filter.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveTextStyle
import com.flixclusive.core.ui.common.util.adaptive.TextStyleMode
import com.flixclusive.core.ui.common.util.adaptive.TypographyStyle

@Composable
internal fun BaseFilterGroup(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style =
                getAdaptiveTextStyle(
                    mode = TextStyleMode.Emphasized,
                    style = TypographyStyle.Headline,
                ),
        )

        content()
    }
}
