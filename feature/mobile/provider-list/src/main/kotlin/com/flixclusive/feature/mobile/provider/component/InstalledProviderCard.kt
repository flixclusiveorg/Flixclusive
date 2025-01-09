package com.flixclusive.feature.mobile.provider.component

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.DummyDataForPreview
import com.flixclusive.core.ui.mobile.component.provider.ProviderTopCardContent
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.Status

@Composable
internal fun InstalledProviderCard(
    providerMetadata: ProviderMetadata,
    enabledProvider: () -> Boolean,
    isDraggableProvider: () -> Boolean,
    displacementOffsetProvider: () -> Float?,
    onClick: () -> Unit,
    openSettings: () -> Unit,
    uninstallProvider: () -> Unit,
    onToggleProvider: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed = interactionSource.collectIsPressedAsState()
    val isDragging =
        remember {
            derivedStateOf {
                displacementOffsetProvider() != null && pressed.value
            }
        }

    val cardScale =
        animateFloatAsState(
            targetValue = if (isDragging.value) 1.15F else 1F,
            label = "CardScale",
        )

    val cardAlpha =
        animateFloatAsState(
            targetValue = if (enabledProvider() || isDragging.value) 1F else 0.8F,
            label = "CardAlpha",
        )

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationY =
                        if (!isDraggableProvider()) 0F else displacementOffsetProvider() ?: 0f

                    alpha = cardAlpha.value
                    scaleX = cardScale.value
                    scaleY = cardScale.value
                },
        contentAlignment = Alignment.Center,
    ) {
        Card(
            onClick = onClick,
            interactionSource = interactionSource,
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                ),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
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
                    isDraggableProvider = isDraggableProvider,
                    providerMetadata = providerMetadata,
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
}

@Preview
@Composable
private fun ProviderCardPreview() {
    val providerMetadata = DummyDataForPreview.getDummyProviderMetadata()

    FlixclusiveTheme {
        Surface {
            InstalledProviderCard(
                providerMetadata = providerMetadata,
                enabledProvider = { true },
                isDraggableProvider = { true },
                displacementOffsetProvider = { null },
                openSettings = {},
                uninstallProvider = {},
                onClick = {},
                onToggleProvider = {},
            )
        }
    }
}
