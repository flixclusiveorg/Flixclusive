package com.flixclusive.feature.mobile.provider.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.gradle.entities.ProviderData
import com.flixclusive.gradle.entities.Status
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.util.R as UtilR

@Composable
internal fun BottomCardContent(
    providerData: ProviderData,
    enabled: Boolean,
    openSettings: () -> Unit,
    unloadProvider: () -> Unit,
    toggleUsage: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = if (providerData.changelog != null) Arrangement.SpaceBetween else Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (providerData.changelog != null) {
            Icon(
                painter = painterResource(id = UiCommonR.drawable.round_update_24),
                contentDescription = stringResource(id = UtilR.string.provider_updates_log),
                tint = LocalContentColor.current.onMediumEmphasis(0.4F),
                modifier = Modifier
                    .size(23.dp)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = unloadProvider,
                modifier = Modifier.size(width = 80.dp, height = 25.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface.onMediumEmphasis(0.4F),
                )
            ) {
                Text(
                    text = stringResource(UtilR.string.uninstall),
                    style = MaterialTheme.typography.labelMedium
                )
            }

            OutlinedButton(
                onClick = openSettings,
                modifier = Modifier.size(width = 50.dp, height = 25.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    painter = painterResource(id = UiCommonR.drawable.provider_settings),
                    contentDescription = stringResource(id = UtilR.string.provider_settings_icon_content_desc),
                    modifier = Modifier
                        .scale(0.8F)
                )
            }

            Switch(
                checked = enabled,
                enabled = providerData.status != Status.Maintenance && providerData.status != Status.Down,
                colors = SwitchDefaults.colors(
                    disabledCheckedThumbColor = if (providerData.status == Status.Maintenance)
                        Color(0xFF331821)
                    else MaterialTheme.colorScheme.surface
                        .onMediumEmphasis(1F)
                        .compositeOver(MaterialTheme.colorScheme.surface),
                    disabledCheckedTrackColor = if (providerData.status == Status.Maintenance)
                        Color(0xFFFC93B7)
                    else MaterialTheme.colorScheme.onSurface
                        .onMediumEmphasis(0.12F)
                        .compositeOver(MaterialTheme.colorScheme.surface),
                ),
                onCheckedChange = {
                    toggleUsage()
                },
                modifier = Modifier
                    .scale(0.7F)
                    .width(40.dp)
            )
        }
    }
}