package com.flixclusive.core.ui.mobile.component.provider

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.DummyDataForPreview
import com.flixclusive.model.provider.ProviderData
import com.flixclusive.model.provider.Status

@Composable
fun InstalledProviderCard(
    modifier: Modifier = Modifier,
    providerData: ProviderData,
    enabled: Boolean,
    isDraggable: Boolean,
    displacementOffset: Float?,
    onClick: () -> Unit,
    openSettings: () -> Unit,
    uninstallProvider: () -> Unit,
    onToggleProvider: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val isBeingDragged = remember(displacementOffset) {
        displacementOffset != null
    }

    val elevation = when {
        isBeingDragged && enabled && isDraggable -> 20.dp
        else -> 3.dp
    }
    val cardColor = MaterialTheme.colorScheme.surfaceColorAtElevation(elevation)

    Box(
        modifier = modifier
            .graphicsLayer { translationY = if (!isDraggable) 0F else displacementOffset ?: 0f }
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            onClick = onClick,
            interactionSource = interactionSource,
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(elevation),
                contentColor = contentColorFor(
                    backgroundColor = cardColor.copy(
                        if (!enabled) 0.4F else 1F
                    )
                )
            ),
            border = if (isBeingDragged && enabled || pressed)
                BorderStroke(
                    width = 2.dp,
                    color = contentColorFor(cardColor)
                ) else null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(
                        horizontal = 15.dp,
                        vertical = 10.dp
                    )
            ) {
                TopCardContent(
                    isDraggable = isDraggable,
                    providerData = providerData,
                )

                HorizontalDivider(
                    modifier = Modifier
                        .padding(top = 15.dp),
                    thickness = 0.5.dp
                )

                BottomCardContent(
                    providerData = providerData,
                    enabled = enabled,
                    openSettings = openSettings,
                    unloadProvider = uninstallProvider,
                    toggleUsage = {
                        if (providerData.status != Status.Maintenance
                            && providerData.status != Status.Down) {
                            onToggleProvider()
                        }
                    }
                )
            }
        }
    }
}

@Preview
@Composable
private fun ProviderCardPreview() {
    val providerData = DummyDataForPreview.getDummyProviderData()

    FlixclusiveTheme {
        Surface {
            InstalledProviderCard(
                providerData = providerData,
                enabled = true,
                isDraggable = true,
                displacementOffset = null,
                openSettings = {},
                uninstallProvider = {},
                onClick = {},
                onToggleProvider = {},
            )
        }
    }
}