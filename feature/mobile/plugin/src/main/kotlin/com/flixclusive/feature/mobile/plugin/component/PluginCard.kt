package com.flixclusive.feature.mobile.plugin.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.feature.mobile.plugin.R
import com.flixclusive.gradle.entities.Author
import com.flixclusive.gradle.entities.Language
import com.flixclusive.gradle.entities.PluginData
import com.flixclusive.gradle.entities.PluginType
import com.flixclusive.gradle.entities.Status
import com.flixclusive.core.util.R as UtilR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PluginCard(
    pluginData: PluginData,
    enabled: Boolean,
    displacementOffset: Float?,
    openSettings: () -> Unit,
    unloadPlugin: () -> Unit,
    onToggleProvider: () -> Unit,
) {
    val isBeingDragged = remember(displacementOffset) {
        displacementOffset != null
    }
    
    val isNotMaintenance = pluginData.status != Status.Maintenance

    val color = if (isBeingDragged && isNotMaintenance) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = Modifier
            .graphicsLayer { translationY = displacementOffset ?: 0f }
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        Card(
            enabled = isNotMaintenance,
            onClick = onToggleProvider,
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = color,
                contentColor = contentColorFor(backgroundColor = color)
            ),
            border = if (isBeingDragged && !isNotMaintenance)
                BorderStroke(
                    width = 2.dp,
                    color = contentColorFor(color)
                ) else null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 15.dp)
            ) {
                TopCardContent(pluginData = pluginData)

                Divider(thickness = 0.5.dp)

                BottomCardContent(
                    pluginData = pluginData,
                    enabled = enabled,
                    openSettings = openSettings,
                    unloadPlugin = unloadPlugin,
                    toggleUsage = onToggleProvider
                )
            }
        }
        Card(
            enabled = isNotMaintenance,
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = color,
                contentColor = contentColorFor(backgroundColor = color)
            ),
            border = if (isBeingDragged && !isNotMaintenance)
                BorderStroke(
                    width = 2.dp,
                    color = contentColorFor(color)
                ) else null,
            onClick = {
                onToggleProvider()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    enabled = isNotMaintenance,
                    checked = enabled && isNotMaintenance,
                    onCheckedChange = {
                        onToggleProvider()
                    },
                    colors = CheckboxDefaults.colors(
                        uncheckedColor = contentColorFor(color)
                    ),
                    modifier = Modifier
                        .padding(2.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1F)
                        .padding(horizontal = 2.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = pluginData.name,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal
                        ),
                    )

                    if (!isNotMaintenance) {
                        Text(
                            text = stringResource(id = UtilR.string.maintenance_all_caps),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                letterSpacing = 2.sp,
                            ),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                Icon(
                    painter = painterResource(id = R.drawable.round_drag_indicator_24),
                    contentDescription = stringResource(UtilR.string.drag_icon_content_desc),
                    modifier = Modifier
                        .padding(2.dp)
                        .padding(end = 5.dp)
                )
            }
        }
    }
}

@Preview
@Composable
private fun ProviderCardPreview() {
    val pluginData = PluginData(
        authors = listOf(Author("FLX")),
        repositoryUrl = null,
        buildUrl = null,
        changelog = null,
        changelogMedia = null,
        versionName = "1.0.0",
        versionCode = 10000,
        description = null,
        iconUrl = null,
        language = Language.Multiple,
        name = "123Movies",
        pluginType = PluginType.All,
        status = Status.Working
    )

    FlixclusiveTheme {
        Surface {
            Card(
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 15.dp)
                ) {
                    TopCardContent(pluginData)

                    Divider(thickness = 0.5.dp)

                    BottomCardContent(
                        pluginData = pluginData,
                        enabled = true,
                        openSettings = {},
                        unloadPlugin = {},
                        toggleUsage = {}
                    )
                }
            }
        }
    }
}