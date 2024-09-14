package com.flixclusive.feature.splashScreen.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun ProvidersDisclaimer(
    modifier: Modifier = Modifier,
    understood: (isOptIn: Boolean) -> Unit
) {
    Consent(
        modifier = modifier,
        header = stringResource(id = LocaleR.string.provider_disclaimer),
        consentContent = stringResource(id = LocaleR.string.disclaimer_provider_message),
        goNext = understood
    )
}