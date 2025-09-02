package com.flixclusive.feature.mobile.provider.add.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.theme.warningColor
import com.flixclusive.core.ui.common.adaptive.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.EmptyDataMessage
import com.flixclusive.core.strings.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

@Composable
internal fun ErrorScreen(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EmptyDataMessage(
        modifier = modifier,
        description = stringResource(LocaleR.string.no_available_providers),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(bottom = 12.dp),
        ) {
            AdaptiveIcon(
                painter = painterResource(UiCommonR.drawable.warning_outline),
                contentDescription = null,
                tint = warningColor, // TODO: Watch out for hardcoded values
                dp = 70.dp,
            )

            OutlinedButton(onClick = onRetry) {
                Text(text = stringResource(LocaleR.string.retry))
            }
        }
    }
}
