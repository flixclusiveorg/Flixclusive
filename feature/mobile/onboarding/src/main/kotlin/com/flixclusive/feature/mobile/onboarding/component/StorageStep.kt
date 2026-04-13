package com.flixclusive.feature.mobile.onboarding.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.feature.mobile.onboarding.R
import com.flixclusive.core.drawables.R as UiCommonR

@Composable
internal fun StorageStep(
    storageDirectoryUri: String?,
    onPickStorageDirectory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.onboarding_storage_title),
            style = MaterialTheme.typography.headlineMedium.asAdaptiveTextStyle(),
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = stringResource(R.string.onboarding_storage_desc),
            style = MaterialTheme.typography.bodyMedium.asAdaptiveTextStyle(),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.onboarding_storage_current_label),
                style = MaterialTheme.typography.labelLarge.asAdaptiveTextStyle(),
                fontWeight = FontWeight.SemiBold,
            )

            Text(
                text = storageDirectoryUri ?: stringResource(R.string.onboarding_storage_not_selected),
                style = MaterialTheme.typography.labelSmall.asAdaptiveTextStyle(),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }


        Button(
            onClick = onPickStorageDirectory,
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(text = stringResource(R.string.onboarding_storage_pick_button))
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = modifier
                .padding(top = 8.dp)
                .fillMaxWidth(),
        ) {
            AdaptiveIcon(
                painter = painterResource(UiCommonR.drawable.info),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                dp = 20.dp
            )

            Text(
                text = stringResource(R.string.onboarding_storage_tip),
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                ).asAdaptiveTextStyle(),
            )
        }

    }
}
