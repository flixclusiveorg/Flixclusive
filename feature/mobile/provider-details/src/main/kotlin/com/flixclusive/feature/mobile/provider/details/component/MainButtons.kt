package com.flixclusive.feature.mobile.provider.details.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.common.provider.ProviderInstallationStatus
import com.flixclusive.core.presentation.mobile.components.provider.ButtonWithProgress
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun MainButtons(
    providerInstallationStatus: ProviderInstallationStatus,
    onToggleInstallationState: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ToggleInstallationButton(
            installationStatus = providerInstallationStatus,
            onToggleInstallationState = onToggleInstallationState,
            modifier = Modifier.weight(1F),
        )

        // TODO: Add like button here
    }
}

@Composable
internal fun ToggleInstallationButton(
    installationStatus: ProviderInstallationStatus,
    onToggleInstallationState: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconForInstallationStatus = remember(installationStatus) {
        when (installationStatus) {
            ProviderInstallationStatus.Outdated -> UiCommonR.drawable.round_update_24
            ProviderInstallationStatus.Installed -> UiCommonR.drawable.outlined_trash
            else -> UiCommonR.drawable.download
        }
    }

    val labelForInstallationStatus = remember(installationStatus) {
        when (installationStatus) {
            ProviderInstallationStatus.Outdated -> LocaleR.string.update_label
            ProviderInstallationStatus.Installed -> LocaleR.string.uninstall
            else -> LocaleR.string.install
        }
    }

    ButtonWithProgress(
        onClick = onToggleInstallationState,
        iconId = iconForInstallationStatus,
        label = stringResource(id = labelForInstallationStatus),
        emphasize = installationStatus.isNotInstalled || installationStatus.isOutdated,
        isLoading = installationStatus.isInstalling,
        contentPadding = PaddingValues(vertical = 5.dp),
        indicatorSize = 16.dp,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun MainButtonsBasePreview() {
    FlixclusiveTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                MainButtons(
                    providerInstallationStatus = ProviderInstallationStatus.NotInstalled,
                    onToggleInstallationState = {},
                    modifier = Modifier.fillMaxWidth(),
                )

                MainButtons(
                    providerInstallationStatus = ProviderInstallationStatus.Installed,
                    onToggleInstallationState = {},
                    modifier = Modifier.fillMaxWidth(),
                )

                MainButtons(
                    providerInstallationStatus = ProviderInstallationStatus.Installing,
                    onToggleInstallationState = {},
                    modifier = Modifier.fillMaxWidth(),
                )

                MainButtons(
                    providerInstallationStatus = ProviderInstallationStatus.Outdated,
                    onToggleInstallationState = {},
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun MainButtonsCompactLandscapePreview() {
    MainButtonsBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun MainButtonsMediumPortraitPreview() {
    MainButtonsBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun MainButtonsMediumLandscapePreview() {
    MainButtonsBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun MainButtonsExtendedPortraitPreview() {
    MainButtonsBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun MainButtonsExtendedLandscapePreview() {
    MainButtonsBasePreview()
}
