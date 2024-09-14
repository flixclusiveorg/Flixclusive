package com.flixclusive.core.ui.mobile.component.provider

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.ui.common.util.getProviderStatusColor
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.model.provider.ProviderData
import com.flixclusive.model.provider.Status
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.locale.R as LocaleR

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
        val color = getProviderStatusColor(providerData.status)

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1F)
        ) {
            Box(
                modifier = Modifier
                    .background(color, CircleShape)
                    .size(7.dp)
            )

            Text(
                text = providerData.status.toString(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = color.onMediumEmphasis(0.4F),
                    fontSize = 11.sp
                )
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
                    contentColor = LocalContentColor.current.onMediumEmphasis(0.4F),
                )
            ) {
                Text(
                    text = stringResource(LocaleR.string.uninstall),
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
                    contentDescription = stringResource(id = LocaleR.string.provider_settings),
                    modifier = Modifier
                        .scale(0.8F)
                )
            }

            Switch(
                checked = enabled,
                enabled = providerData.status != Status.Maintenance && providerData.status != Status.Down,
                colors = SwitchDefaults.colors(
                    disabledCheckedThumbColor = MaterialTheme.colorScheme.surface
                        .onMediumEmphasis(1F)
                        .compositeOver(MaterialTheme.colorScheme.surface),
                    disabledCheckedTrackColor = MaterialTheme.colorScheme.onSurface
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