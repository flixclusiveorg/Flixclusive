package com.flixclusive.feature.splashScreen.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.flixclusive.core.util.R as UtilR

@Composable
fun ProvidersDisclaimer(
    modifier: Modifier = Modifier,
    understood: (isOptIn: Boolean) -> Unit
) {
    Consent(
        modifier = modifier,
        header = stringResource(id = UtilR.string.provider_disclaimer),
        consentContent = stringResource(id = UtilR.string.disclaimer_provider_message),
        goNext = understood
    )
}