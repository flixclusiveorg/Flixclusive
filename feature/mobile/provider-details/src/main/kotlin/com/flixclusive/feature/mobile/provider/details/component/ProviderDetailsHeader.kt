package com.flixclusive.feature.mobile.provider.details.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.components.ImageWithSmallPlaceholder
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.domain.provider.util.extractGithubInfoFromLink
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.core.drawables.R as UiCommonR

@Composable
internal fun ProviderDetailsHeader(
    provider: ProviderMetadata,
    onRepositoryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (owner, repository) = remember {
        extractGithubInfoFromLink(provider.repositoryUrl) ?: (null to null)
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ImageWithSmallPlaceholder(
            modifier = Modifier.size(50.dp),
            placeholderSize = 25.dp,
            urlImage = provider.iconUrl,
            placeholder = painterResource(UiCommonR.drawable.provider_logo),
            contentDescription = provider.name,
            shape = MaterialTheme.shapes.small,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = provider.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                modifier = Modifier.fillMaxWidth(),
            )

            if (owner != null && repository != null) {
                val repoName = "$owner/$repository"

                Text(
                    text = repoName,
                    style = MaterialTheme.typography.labelSmall.asAdaptiveTextStyle(),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onRepositoryClick),
                )
            }
        }
    }
}

@Preview
@Composable
private fun ProviderDetailsHeaderBasePreview() {
    val providerMetadata = DummyDataForPreview.getProviderMetadata()

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
                    onRepositoryClick = {},
                )

                ProviderDetailsHeader(
                    provider = providerMetadata,
                    onRepositoryClick = {},
                )

                ProviderDetailsHeader(
                    provider = providerMetadata,
                    onRepositoryClick = {},
                )

                ProviderDetailsHeader(
                    provider = providerMetadata,
                    onRepositoryClick = {},
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
