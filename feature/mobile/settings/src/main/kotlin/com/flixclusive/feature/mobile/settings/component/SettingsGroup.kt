package com.flixclusive.feature.mobile.settings.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.feature.mobile.settings.SettingsItem

@Composable
internal fun SettingsGroup(
    items: List<SettingsItem>,
    onItemClick: (SettingsItem) -> Unit = {},
) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                shape = RoundedCornerShape(10.dp)
            )
    ) {
        Column {
            Spacer(modifier = Modifier.padding(vertical = 5.dp))

            items.forEachIndexed { i, item ->
                BaseItemButton(
                    title = item.title,
                    description = item.description?.replace("_", " "),
                    content = item.content,
                    enabled = item.enabled,
                    onClick = {
                        onItemClick(item)
                    }
                )

                if (i < items.lastIndex)
                    HorizontalDivider(
                        modifier = Modifier
                            .padding(horizontal = 10.dp),
                        thickness = 0.5.dp,
                        color = LocalContentColor.current.onMediumEmphasis(emphasis = 0.15F)
                    )
            }

            Spacer(modifier = Modifier.padding(vertical = 5.dp))
        }
    }
}