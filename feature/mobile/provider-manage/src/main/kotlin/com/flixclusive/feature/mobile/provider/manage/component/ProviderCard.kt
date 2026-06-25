package com.flixclusive.feature.mobile.provider.manage.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.common.provider.extensions.asStatusColor
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.components.ImageWithSmallPlaceholder
import com.flixclusive.core.presentation.mobile.components.material3.PlainTooltipBox
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.feature.mobile.provider.manage.CapabilityUiItem
import com.flixclusive.feature.mobile.provider.manage.ProviderWithCapabilities
import com.flixclusive.feature.mobile.provider.manage.R
import com.flixclusive.model.provider.ProviderStatus
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

private val ProviderCardMinHeight = 70.dp
private val ProviderCardIconSize = 40.dp

private val ProviderCardActionButtonSize = 30.dp
private val ProviderCardActionIconSize = 20.dp

@Composable
internal fun ProviderCard(
    provider: ProviderWithCapabilities,
    onClick: () -> Unit,
    openSettings: () -> Unit,
    onUninstall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusColor = provider.metadata.status.asStatusColor()

    Card(
        onClick = onClick,
        modifier = modifier
            .heightIn(ProviderCardMinHeight),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
        ),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .padding(horizontal = 10.dp)
        ) {
            ImageWithSmallPlaceholder(
                placeholderSize = 22.dp,
                urlImage = provider.metadata.iconUrl,
                placeholder = painterResource(UiCommonR.drawable.provider_logo),
                contentDescription = provider.name,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .padding(
                        top = (ProviderCardMinHeight / 2) - (ProviderCardIconSize / 2)
                    ).size(ProviderCardIconSize),
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier
                    .weight(1F)
                    .padding(start = 5.dp, end = 10.dp)
                    .padding(vertical = 10.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = provider.name,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f, false)
                            .padding(bottom = 2.dp),
                    )

                    Text(
                        text = "v${provider.metadata.versionName}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = LocalContentColor.current.copy(0.4F),
                        ),
                    )

                    if (provider.metadata.status != ProviderStatus.Working) {
                        TextChip(
                            label = provider.metadata.status.toString(),
                            color = statusColor,
                        )
                    }
                }

                if (provider.capabilities.isNotEmpty()) {
                    FlowRow {
                        provider.capabilities.fastForEach { capabilityItem ->
                            val capabilityColor by animateColorAsState(
                                targetValue = if (capabilityItem.isEnabled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(0.4f)
                                },
                            )

                            TextChip(
                                label = capabilityItem.label.asString(),
                                color = capabilityColor,
                                modifier = Modifier.padding(end = 4.dp, bottom = 4.dp),
                            )
                        }
                    }
                } else {
                    TextChip(
                        label = stringResource(R.string.label_no_capabilities),
                        color = MaterialTheme.colorScheme.onSurface.copy(0.6f),
                        modifier = Modifier.padding(end = 4.dp, bottom = 4.dp),
                    )
                }
            }

            ActionButton(
                icon = painterResource(id = UiCommonR.drawable.provider_settings),
                contentDescription = stringResource(id = LocaleR.string.provider_settings),
                onClick = openSettings,
                modifier = Modifier.padding(end = 5.dp),
            )

            ActionButton(
                icon = painterResource(id = UiCommonR.drawable.delete_outlined),
                contentDescription = stringResource(id = LocaleR.string.label_uninstall),
                tint = MaterialTheme.colorScheme.error,
                onClick = onUninstall,
            )
        }
    }
}

@Composable
private fun TextChip(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val formattedLabel = remember {
        label.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        }
    }

    Text(
        text = formattedLabel,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = color,
        style = MaterialTheme.typography.labelMedium,
        modifier = modifier
            .background(
                color = color.copy(alpha = 0.15f),
                shape = MaterialTheme.shapes.extraSmall,
            ).padding(horizontal = 5.dp)
    )
}

@Composable
private fun ActionButton(
    icon: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    PlainTooltipBox(description = contentDescription) {
        IconButton(
            onClick = onClick,
            modifier = modifier
                .padding(
                    top = (ProviderCardMinHeight / 2) - (ProviderCardActionButtonSize / 2)
                ).size(ProviderCardActionButtonSize),
        ) {
            Icon(
                painter = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(ProviderCardActionIconSize)
            )
        }
    }
}

@Preview
@Composable
private fun ProviderCardPreview() {
    FlixclusiveTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ProviderCard(
                    onClick = {},
                    onUninstall = {},
                    openSettings = {},
                    provider = ProviderWithCapabilities(
                        metadata = DummyDataForPreview.getProviderMetadata(
                            status = ProviderStatus.Beta,
                            name = "Example Provider with a very long name that should be truncated",
                        ),
                        capabilities = remember {
                            listOf(
                                "Catalogs",
                                "Media links",
                                "Search",
                                "Track",
                                "Cross-match"
                            ).map { CapabilityUiItem(label = UiText.from(it), isEnabled = true) }
                        },
                    ),
                )
                ProviderCard(
                    onClick = {},
                    onUninstall = {},
                    openSettings = {},
                    provider = ProviderWithCapabilities(
                        metadata = DummyDataForPreview.getProviderMetadata(),
                        capabilities = remember {
                            listOf(
                                "Catalogs",
                                "Media links",
                                "Search",
                                "Track",
                                "Cross-match"
                            ).map { CapabilityUiItem(label = UiText.from(it), isEnabled = true) }
                        },
                    ),
                )

                ProviderCard(
                    onClick = {},
                    onUninstall = {},
                    openSettings = {},
                    provider = ProviderWithCapabilities(
                        metadata = DummyDataForPreview.getProviderMetadata(
                            status = ProviderStatus.Beta
                        ),
                        capabilities = remember {
                            listOf(
                                "Catalogs",
                                "Media links",
                            ).map { CapabilityUiItem(label = UiText.from(it), isEnabled = false) }
                        },
                    ),
                )

                ProviderCard(
                    onClick = {},
                    onUninstall = {},
                    openSettings = {},
                    provider = ProviderWithCapabilities(
                        metadata = DummyDataForPreview.getProviderMetadata(
                            status = ProviderStatus.Down
                        ),
                        capabilities = emptyList(),
                    ),
                )
            }
        }
    }
}
