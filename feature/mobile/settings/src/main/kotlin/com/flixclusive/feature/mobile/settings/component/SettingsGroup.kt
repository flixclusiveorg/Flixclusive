package com.flixclusive.feature.mobile.settings.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.feature.mobile.settings.SettingsItem
import com.flixclusive.feature.mobile.settings.settingItemShape

@Composable
internal fun SettingsGroup(
    items: List<SettingsItem>,
    onItemClick: (SettingsItem) -> Unit = {},
) {
    Surface(
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.background,
        shape = settingItemShape
    ) {
        Column {
            Spacer(modifier = Modifier.padding(vertical = 5.dp))

            items.forEachIndexed { i, item ->
                SettingsGroupItem(
                    title = item.title,
                    description = item.description?.replace("_", " "),
                    previewContent = item.previewContent,
                    enabled = item.enabled,
                    onClick = {
                        onItemClick(item)
                    }
                )

                if (i < items.lastIndex)
                    Divider(
                        color = LocalContentColor.current.onMediumEmphasis(emphasis = 0.15F),
                        thickness = 0.5.dp,
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                    )
            }

            Spacer(modifier = Modifier.padding(vertical = 5.dp))
        }
    }
}