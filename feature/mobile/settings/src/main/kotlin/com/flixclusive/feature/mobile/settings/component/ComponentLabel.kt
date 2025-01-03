package com.flixclusive.feature.mobile.settings.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveTextStyle
import com.flixclusive.core.ui.common.util.adaptive.TextStyleMode
import com.flixclusive.core.ui.common.util.adaptive.TypographyStyle

@Composable
internal fun GroupLabel(
    title: String,
    description: String?,
    titleStyle: TextStyle = getAdaptiveTextStyle(
        style = TypographyStyle.Title,
        mode = TextStyleMode.SemiEmphasized,
    ).copy(LocalContentColor.current),
    descriptionStyle: TextStyle = getAdaptiveTextStyle(
        style = TypographyStyle.Body,
        mode = TextStyleMode.NonEmphasized,
    ),
    modifier: Modifier = Modifier,
) {
    if (title.isNotBlank()) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = title,
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
                style = titleStyle,
            )

            if (description?.isNotBlank() == true) {
                Text(
                    text = description,
                    style = descriptionStyle,
                    maxLines = 10,
                )
            }
        }
    }
}