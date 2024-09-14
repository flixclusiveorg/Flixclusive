package com.flixclusive.feature.mobile.provider.info.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.DummyDataForPreview.getDummyProviderData
import com.flixclusive.core.ui.mobile.component.ImageWithSmallPlaceholder
import com.flixclusive.domain.provider.util.extractGithubInfoFromLink
import com.flixclusive.model.provider.ProviderData
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.locale.R as LocaleR


@Composable
internal fun ProviderInfoHeader(
    modifier: Modifier = Modifier,
    providerData: ProviderData,
    openRepositoryScreen: () -> Unit
) {
    val (_, repository) =  remember {
        extractGithubInfoFromLink(providerData.repositoryUrl ?: "") ?: (null to null)
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ImageWithSmallPlaceholder(
            modifier = Modifier.size(65.dp),
            placeholderModifier = Modifier.size(38.dp),
            urlImage = providerData.iconUrl,
            placeholderId = UiCommonR.drawable.provider_logo,
            contentDescId = LocaleR.string.provider_icon_content_desc,
            shape = MaterialTheme.shapes.small,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            Text(
                text = providerData.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
            )

            repository?.let {
                Text(
                    text = it,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    ),
                    modifier = Modifier
                        .clickable(onClick = openRepositoryScreen)
                )
            }

        }
    }

}


@Preview
@Composable
private fun ProviderSettingsHeaderPreview() {
    val providerData = getDummyProviderData()
    
    FlixclusiveTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
            ) {
                ProviderInfoHeader(
                    providerData = providerData,
                    openRepositoryScreen = {}
                )
            }
        }
    }
}