package com.flixclusive.feature.mobile.provider.add.filter.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveTextStyle
import com.flixclusive.core.ui.common.util.adaptive.TextStyleMode
import com.flixclusive.core.ui.common.util.onMediumEmphasis

@Composable
internal fun BaseTextButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    prefix: @Composable RowScope.() -> Unit,
) {
    Box(
        modifier =
            modifier
                .clickable(onClick = onClick)
                .fillMaxWidth()
                .minimumInteractiveComponentSize(),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            prefix()

            Text(
                text = label,
                style =
                    getAdaptiveTextStyle(
                        mode = TextStyleMode.SemiEmphasized,
                        size = 15.sp,
                    ).copy(
                        color = LocalContentColor.current.onMediumEmphasis(if (isSelected) 1F else 0.6F),
                    ),
                modifier =
                    Modifier
                        .weight(1F)
                        .align(Alignment.CenterVertically),
            )
        }
    }
}
