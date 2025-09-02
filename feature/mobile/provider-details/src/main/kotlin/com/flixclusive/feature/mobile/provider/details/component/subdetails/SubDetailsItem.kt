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
import com.flixclusive.core.presentation.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.feature.mobile.provider.details.LABEL_SIZE_IN_SP
import com.flixclusive.feature.mobile.provider.details.SUB_LABEL_SIZE

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
            style = MaterialTheme.typography.titleMedium.copy(
                color = LocalContentColor.current.copy(0.8F),
                fontWeight = FontWeight.Bold,
                fontSize = LABEL_SIZE_IN_SP
            )
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = LocalContentColor.current.copy(0.6F),
                fontWeight = FontWeight.Normal,
                fontSize = SUB_LABEL_SIZE
            )
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
