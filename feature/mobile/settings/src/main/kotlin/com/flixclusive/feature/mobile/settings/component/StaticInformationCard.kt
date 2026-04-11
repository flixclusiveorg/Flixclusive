package com.flixclusive.feature.mobile.settings.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.feature.mobile.settings.TweakPaddingHorizontal
import com.flixclusive.core.drawables.R as UiCommonR


@Composable
internal fun StaticInformationCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    val horizontalPadding = getAdaptiveDp(TweakPaddingHorizontal * 1.8F)
    val contentPadding = getAdaptiveDp(12.dp)

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .padding(horizontal = horizontalPadding)
            .fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(getAdaptiveDp(12.dp)),
            modifier = Modifier.padding(contentPadding),
        ) {
            AdaptiveIcon(
                painter = painterResource(UiCommonR.drawable.info),
                contentDescription = title.ifEmpty { description },
                tint = MaterialTheme.colorScheme.primary.copy(0.6f),
            )

            TitleDescriptionHeader(
                title = title,
                descriptionProvider = { description },
                titleStyle = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                descriptionStyle = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.75f),
                ),
            )
        }
    }
}
