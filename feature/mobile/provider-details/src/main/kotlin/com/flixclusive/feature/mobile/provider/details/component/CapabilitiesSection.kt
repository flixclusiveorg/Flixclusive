package com.flixclusive.feature.mobile.provider.details.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.components.provider.ProviderInstallState
import com.flixclusive.data.provider.ProviderCapability
import com.flixclusive.feature.mobile.provider.details.CapabilityItem
import com.flixclusive.feature.mobile.provider.details.R

private val AnimationDuration = 300

@Composable
internal fun CapabilitiesSection(
    installState: () -> ProviderInstallState,
    capabilities: () -> List<CapabilityItem>,
    onToggleCapability: (ProviderCapability) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isVisible by remember {
        derivedStateOf {
            installState() is ProviderInstallState.Installed
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(
            animationSpec = tween(durationMillis = AnimationDuration),
            expandFrom = Alignment.Top,
        ) + fadeIn(animationSpec = tween(durationMillis = AnimationDuration)),
        exit = shrinkVertically(
            animationSpec = tween(durationMillis = AnimationDuration),
            shrinkTowards = Alignment.Top,
        ) + fadeOut(animationSpec = tween(durationMillis = AnimationDuration)),
        modifier = modifier,
    ) {
        Column {
            SectionLabel(
                text = stringResource(R.string.label_capabilities),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                capabilities().forEach { item ->
                    ProviderCapabilityToggleItem(
                        item = item,
                        onToggle = { onToggleCapability(item.capability) },
                    )
                }
            }

            ProviderDetailsDivider(
                modifier = Modifier.padding(vertical = 10.dp),
            )
        }
    }
}
