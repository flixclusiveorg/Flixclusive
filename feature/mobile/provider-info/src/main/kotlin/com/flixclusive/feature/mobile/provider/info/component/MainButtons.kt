package com.flixclusive.feature.mobile.provider.info.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.mobile.component.provider.ButtonWithCircularProgressIndicator
import com.flixclusive.core.ui.mobile.component.provider.ProviderInstallationStatus
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun MainButtons(
    modifier: Modifier = Modifier,
    providerInstallationStatus: ProviderInstallationStatus,
    onToggleInstallationState: () -> Unit,
    onTestProvider: () -> Unit
) {
    val iconForInstallationStatus = remember(providerInstallationStatus) {
        when (providerInstallationStatus) {
            ProviderInstallationStatus.Installed -> UiCommonR.drawable.outlined_trash
            ProviderInstallationStatus.Outdated -> UiCommonR.drawable.round_update_24
            else -> UiCommonR.drawable.download
        }
    }

    val labelForInstallationStatus = remember(providerInstallationStatus) {
        when (providerInstallationStatus) {
            ProviderInstallationStatus.Installed -> LocaleR.string.uninstall
            ProviderInstallationStatus.Outdated -> LocaleR.string.update_label
            else -> LocaleR.string.install
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ButtonWithCircularProgressIndicator(
            onClick = onTestProvider,
            enabled = providerInstallationStatus.isInstalled,
            iconId = UiCommonR.drawable.test,
            emphasize = providerInstallationStatus.isInstalling,
            label = stringResource(id = LocaleR.string.test_provider),
            contentPadding = PaddingValues(vertical = 5.dp),
            modifier = Modifier.weight(1F)
        )

        ButtonWithCircularProgressIndicator(
            onClick = onToggleInstallationState,
            iconId = iconForInstallationStatus,
            label = stringResource(id = labelForInstallationStatus),
            emphasize = providerInstallationStatus.isNotInstalled,
            isLoading = providerInstallationStatus.isInstalling,
            contentPadding = PaddingValues(vertical = 5.dp),
            indicatorSize = 16.dp,
            modifier = Modifier.weight(1F)
        )
    }
}

@Preview
@Composable
private fun CustomButtonPreview() {
    FlixclusiveTheme {
        Surface {

        }
    }
}