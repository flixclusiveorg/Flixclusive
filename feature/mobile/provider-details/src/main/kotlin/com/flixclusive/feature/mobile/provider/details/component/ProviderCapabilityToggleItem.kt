package com.flixclusive.feature.mobile.provider.details.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.data.provider.ProviderCapability
import com.flixclusive.feature.mobile.provider.details.CapabilityItem

@Composable
internal fun ProviderCapabilityToggleItem(
    item: CapabilityItem,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resources = LocalResources.current

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable { onToggle() }
            .padding(horizontal = 5.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(1.dp),
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
        ) {
            Text(
                text = item.label.asString(resources),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = item.description.asString(resources),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }

        Switch(
            checked = item.isEnabled,
            onCheckedChange = { onToggle() },
        )
    }
}

@Preview
@Composable
private fun ProviderCapabilityToggleItemPreview() {
    FlixclusiveTheme {
        Surface {
            ProviderCapabilityToggleItem(
                item = CapabilityItem(
                    capability = ProviderCapability.SEARCH,
                    label = UiText.from("Search"),
                    description = UiText.from("Searches for media items"),
                    isEnabled = true,
                ),
                onToggle = {},
            )
        }
    }
}
