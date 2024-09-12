package com.flixclusive.feature.splashScreen.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.flixclusive.core.util.R as UtilR

@Composable
fun PrivacyNotice(
    modifier: Modifier = Modifier,
    nextStep: (isOptIn: Boolean) -> Unit
) {
    Consent(
        modifier = modifier,
        header = stringResource(id = UtilR.string.privacy_notice),
        consentContent = stringResource(id = UtilR.string.privacy_notice_crash_log_sender),
        optInLabel = stringResource(id = UtilR.string.privacy_notice_opt_in),
        goNext = nextStep
    )
}