package com.flixclusive.feature.mobile.provider.details.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme

@Composable
internal fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    val uppercasedText = remember {
        text.uppercase()
    }

    Text(
        text = uppercasedText,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Black,
        color = LocalContentColor.current.copy(0.5f),
        modifier = modifier
    )
}

@Preview
@Composable
private fun SectionLabelPreview() {
    FlixclusiveTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SectionLabel(text = "Description")
                SectionLabel(text = "Test")
                SectionLabel(text = "Changelogs")
                SectionLabel(text = "What's new")
                SectionLabel(text = "Capabilities")
            }
        }
    }
}
