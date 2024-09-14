package com.flixclusive.feature.splashScreen.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun PrivacyNotice(
    modifier: Modifier = Modifier,
    nextStep: (isOptIn: Boolean) -> Unit
) {
    Consent(
        modifier = modifier,
        header = stringResource(id = LocaleR.string.privacy_notice),
        consentContent = stringResource(id = LocaleR.string.privacy_notice_crash_log_sender),
        optInLabel = stringResource(id = LocaleR.string.privacy_notice_opt_in),
        goNext = nextStep
    )
}