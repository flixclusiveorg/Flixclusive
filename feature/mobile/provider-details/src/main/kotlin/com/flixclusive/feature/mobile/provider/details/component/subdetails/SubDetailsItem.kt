package com.flixclusive.feature.mobile.provider.details.component.subdetails

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.feature.mobile.provider.details.util.ProviderDetailsUiCommon.LABEL_SIZE_IN_SP
import com.flixclusive.feature.mobile.provider.details.util.ProviderDetailsUiCommon.SUB_LABEL_SIZE

@Composable
internal fun SubDetailsItem(
    title: String,
    subtitle: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = LocalContentColor.current.copy(0.8F),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium.asAdaptiveTextStyle(LABEL_SIZE_IN_SP)
        )

        Text(
            text = subtitle,
            color = LocalContentColor.current.copy(0.6F),
            style = MaterialTheme.typography.bodyMedium.asAdaptiveTextStyle(SUB_LABEL_SIZE)
        )
    }
}

@Preview
@Composable
private fun DetailBlockPreview() {
    FlixclusiveTheme {
        Surface {
            SubDetailsItem(
                title = "Title",
                subtitle = "Subtitle"
            )
        }
    }
}
