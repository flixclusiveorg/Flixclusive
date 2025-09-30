package com.flixclusive.feature.mobile.settings.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle

// TODO: Optimize
@Composable
internal fun TitleDescriptionHeader(
    title: String,
    descriptionProvider: (() -> String)?,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = MaterialTheme.typography.titleMedium.asAdaptiveTextStyle(),
    descriptionStyle: TextStyle = MaterialTheme.typography.bodySmall
        .copy(color = LocalContentColor.current.copy(0.6f))
        .asAdaptiveTextStyle(),
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

            if (descriptionProvider?.invoke()?.isNotEmpty() == true) {
                Text(
                    text = descriptionProvider(),
                    style = descriptionStyle,
                    maxLines = 10,
                )
            }
        }
    }
}
