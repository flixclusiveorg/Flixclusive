package com.flixclusive.feature.mobile.provider.manage.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.flixclusive.core.ui.mobile.component.provider.ProviderTopCardContent
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.Status

@Composable
internal fun InstalledProviderCard(
    providerMetadata: ProviderMetadata,
    interactionSource: MutableInteractionSource,
    enabledProvider: () -> Boolean,
    isDraggable: Boolean,
    isDraggingProvider: () -> Boolean,
    onClick: () -> Unit,
    openSettings: () -> Unit,
    uninstallProvider: () -> Unit,
    onToggleProvider: () -> Unit,
    modifier: Modifier = Modifier,
    dragModifier: Modifier = Modifier,
) {
    val cardAlpha =
        animateFloatAsState(
            targetValue = if ((isDraggingProvider() && isDraggable) || enabledProvider()) 1F else 0.6F,
            label = "CardAlpha",
        )

    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        interactionSource = interactionSource,
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
            ),
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp)
                .graphicsLayer {
                    alpha = cardAlpha.value
                },
    ) {
        Column(
            modifier =
                Modifier
                    .padding(
                        horizontal = 15.dp,
                        vertical = 10.dp,
                    ),
        ) {
            ProviderTopCardContent(
                isDraggable = isDraggable,
                providerMetadata = providerMetadata,
                dragModifier = dragModifier,
            )

            HorizontalDivider(
                modifier =
                    Modifier
                        .padding(top = 15.dp),
                thickness = 0.5.dp,
            )

            ProviderBottomCardContent(
                providerMetadata = providerMetadata,
                enabledProvider = enabledProvider,
                openSettings = openSettings,
                unloadProvider = uninstallProvider,
                toggleUsage = {
                    if (providerMetadata.status != Status.Maintenance &&
                        providerMetadata.status != Status.Down
                    ) {
                        onToggleProvider()
                    }
                },
            )
        }
    }
}
