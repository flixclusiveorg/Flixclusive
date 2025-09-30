package com.flixclusive.feature.mobile.provider.manage.component

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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.Status
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@Stable
private fun getProviderStatusColor(status: Status) =
    when (status) {
        Status.Down -> Color(0xFFFF3030)
        Status.Maintenance -> Color(0xFFFFBF1B)
        Status.Beta -> Color(0xFF00C4FF)
        Status.Working -> Color(0xFF00FF04)
    }

@Composable
internal fun ProviderBottomCardContent(
    providerMetadata: ProviderMetadata,
    enabledProvider: () -> Boolean,
    openSettings: () -> Unit,
    unloadProvider: () -> Unit,
    toggleUsage: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (providerMetadata.changelog != null) Arrangement.SpaceBetween else Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = getProviderStatusColor(providerMetadata.status)

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1F),
        ) {
            Box(
                modifier = Modifier
                    .background(color, CircleShape)
                    .size(7.dp),
            )

            Text(
                text = providerMetadata.status.toString(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Black,
                color = color.copy(0.4F),
                style = MaterialTheme.typography.labelSmall.asAdaptiveTextStyle(11.sp),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = unloadProvider,
                modifier = Modifier.size(width = 80.dp, height = 25.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = LocalContentColor.current.copy(0.4F),
                ),
            ) {
                Text(
                    text = stringResource(LocaleR.string.uninstall),
                    style = MaterialTheme.typography.labelMedium.asAdaptiveTextStyle(),
                )
            }

            OutlinedButton(
                onClick = openSettings,
                modifier = Modifier.size(width = 50.dp, height = 25.dp),
                contentPadding = PaddingValues(0.dp),
            ) {
                AdaptiveIcon(
                    painter = painterResource(id = UiCommonR.drawable.provider_settings),
                    contentDescription = stringResource(id = LocaleR.string.provider_settings),
                    dp = 15.dp,
                )
            }

            Switch(
                checked = enabledProvider(),
                enabled = providerMetadata.status != Status.Maintenance && providerMetadata.status != Status.Down,
                colors = SwitchDefaults.colors(
                    disabledCheckedThumbColor =
                        MaterialTheme.colorScheme.surface
                            .copy(1F)
                            .compositeOver(MaterialTheme.colorScheme.surface),
                    disabledCheckedTrackColor =
                        MaterialTheme.colorScheme.onSurface
                            .copy(0.12F)
                            .compositeOver(MaterialTheme.colorScheme.surface),
                ),
                onCheckedChange = { toggleUsage() },
                modifier = Modifier
                    .scale(0.7F)
                    .width(40.dp),
            )
        }
    }
}
