package com.flixclusive.feature.mobile.provider.details.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.common.provider.ProviderInstallationStatus
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.components.ImageWithSmallPlaceholder
import com.flixclusive.core.presentation.mobile.extensions.isCompact
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.domain.provider.util.extractGithubInfoFromLink
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun ProviderDetailsHeader(
    provider: ProviderMetadata,
    installationStatus: ProviderInstallationStatus,
    onToggleInstallationState: () -> Unit,
    openRepositoryScreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val windowWidthSizeClass = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass
    val (owner, repository) = remember {
        extractGithubInfoFromLink(provider.repositoryUrl) ?: (null to null)
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ImageWithSmallPlaceholder(
            modifier = Modifier.size(65.dp),
            placeholderSize = 38.dp,
            urlImage = provider.iconUrl,
            placeholderId = UiCommonR.drawable.provider_logo,
            contentDescId = LocaleR.string.provider_icon_content_desc,
            shape = MaterialTheme.shapes.small,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = provider.name,
                style = MaterialTheme.typography.titleMedium.asAdaptiveTextStyle(),
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )

            if (owner != null && repository != null) {
                val repoName = "$owner/$repository"

                Text(
                    text = repoName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall.asAdaptiveTextStyle(),
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = openRepositoryScreen),
                )
            }
        }

        // Installation button is not shown in compact mode because it's shown in the bottom bar
        if (!windowWidthSizeClass.isCompact) {
            ToggleInstallationButton(
                installationStatus = installationStatus,
                onToggleInstallationState = onToggleInstallationState,
                modifier = Modifier.width(getAdaptiveDp(100.dp, 35.dp)),
            )
        }
    }
}

@Preview
@Composable
private fun ProviderDetailsHeaderBasePreview() {
    val providerMetadata = DummyDataForPreview.getDummyProviderMetadata()

    FlixclusiveTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .padding(horizontal = 10.dp),
            ) {
                ProviderDetailsHeader(
                    provider = providerMetadata,
                    installationStatus = ProviderInstallationStatus.NotInstalled,
                    onToggleInstallationState = {},
                    openRepositoryScreen = {},
                )

                ProviderDetailsHeader(
                    provider = providerMetadata,
                    installationStatus = ProviderInstallationStatus.Installed,
                    onToggleInstallationState = {},
                    openRepositoryScreen = {},
                )

                ProviderDetailsHeader(
                    provider = providerMetadata,
                    installationStatus = ProviderInstallationStatus.Installing,
                    onToggleInstallationState = {},
                    openRepositoryScreen = {},
                )

                ProviderDetailsHeader(
                    provider = providerMetadata,
                    installationStatus = ProviderInstallationStatus.Outdated,
                    onToggleInstallationState = {},
                    openRepositoryScreen = {},
                )
            }
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun ProviderDetailsHeaderCompactLandscapePreview() {
    ProviderDetailsHeaderBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun ProviderDetailsHeaderMediumPortraitPreview() {
    ProviderDetailsHeaderBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun ProviderDetailsHeaderMediumLandscapePreview() {
    ProviderDetailsHeaderBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun ProviderDetailsHeaderExtendedPortraitPreview() {
    ProviderDetailsHeaderBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun ProviderDetailsHeaderExtendedLandscapePreview() {
    ProviderDetailsHeaderBasePreview()
}
