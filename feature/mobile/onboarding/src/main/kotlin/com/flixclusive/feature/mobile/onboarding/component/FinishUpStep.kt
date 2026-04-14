package com.flixclusive.feature.mobile.onboarding.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.feature.mobile.onboarding.R

@Composable
internal fun FinishUpStep(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.Start,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = "\uD83C\uDFC1",
            style = MaterialTheme.typography.displayMedium.asAdaptiveTextStyle(),
            modifier = Modifier.padding(bottom = 6.dp),
        )

        Text(
            text = stringResource(R.string.onboarding_finish_up_title),
            style = MaterialTheme.typography.headlineMedium.asAdaptiveTextStyle(),
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = stringResource(R.string.onboarding_finish_up_desc),
            style = MaterialTheme.typography.bodyMedium.asAdaptiveTextStyle(),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
        )
    }
}
