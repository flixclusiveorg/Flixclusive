package com.flixclusive.feature.mobile.provider.add.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.components.provider.ProviderInstallButton
import com.flixclusive.core.presentation.mobile.components.provider.ProviderInstallState
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.ProviderStatus

@Composable
internal fun ProviderCard(
    provider: ProviderMetadata,
    installState: () -> ProviderInstallState,
    onClick: () -> Unit,
    onUninstall: () -> Unit,
    onConfigure: () -> Unit,
    onRepositoryClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small,
    isSelected: Boolean = false,
) {
    val selectedColor = MaterialTheme.colorScheme.primary
    val borderColor by animateColorAsState(
        when {
            isSelected -> selectedColor
            else -> Color.Transparent
        }
    )

    val borderWidth by animateDpAsState(
        when {
            provider.status != ProviderStatus.Working || isSelected -> 1.5.dp
            else -> 0.dp
        }
    )

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> selectedColor.copy(alpha = 0.12f)
                else -> MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
            },
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = borderWidth,
                color = borderColor,
                shape = shape,
            ),
    ) {
        Column(
            modifier = Modifier
                .padding(
                    horizontal = 15.dp,
                ),
        ) {
            ProviderTopCardContent(
                provider = provider,
                onRepositoryClick = {
                    if (!isSelected) onRepositoryClick()
                },
                modifier = Modifier
                    .padding(top = 10.dp),
            )

            HorizontalDivider(
                modifier = Modifier
                    .padding(vertical = 15.dp),
                thickness = 0.5.dp,
            )

            provider.description?.takeIf { it.isNotEmpty() }?.let {
                Text(
                    text = it,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 3,
                    color = LocalContentColor.current.copy(0.9f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }

            ProviderInstallButton(
                state = installState,
                onToggleInstallState = onClick,
                onUninstall = onUninstall,
                onConfigure = onConfigure,
                enabled = !isSelected
            )
        }
    }
}

@Preview
@Composable
private fun ProviderCardPreview() {
    val providerMetadata = DummyDataForPreview.getProviderMetadata(
        status = ProviderStatus.Down
    )

    FlixclusiveTheme {
        Surface {
            ProviderCard(
                provider = providerMetadata,
                isSelected = true,
                installState = {
                    ProviderInstallState.Outdated(
                        newVersion = "2.0.0",
                        newChangelogs = """
                        - New feature added
                        - Bug fixes and performance improvements
                        """.trimIndent()
                    )
                },
                onClick = {},
                onUninstall = {},
                onConfigure = {},
                onRepositoryClick = {},
            )
        }
    }
}
