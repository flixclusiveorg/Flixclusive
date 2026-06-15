package com.flixclusive.feature.mobile.provider.details.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun ProviderDetailsDescription(
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SectionLabel(text = stringResource(id = LocaleR.string.description))

        Text(
            text = description,
            overflow = TextOverflow.Ellipsis,
            color = LocalContentColor.current.copy(0.9f),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}

@Preview
@Composable
private fun ProviderDetailsDescriptionPreview() {
    FlixclusiveTheme {
        Surface {
            ProviderDetailsDescription(
                description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                    "Donec auctor, nisl eget ultricies lacinia, nunc nisl aliquam nisl, eget " +
                    "aliquam nisl nunc eget nisl. Donec auctor, nisl eget ultricies lacinia, nunc nisl " +
                    "aliquam nisl, eget aliquam nisl nunc eget nisl."
            )
        }
    }
}
