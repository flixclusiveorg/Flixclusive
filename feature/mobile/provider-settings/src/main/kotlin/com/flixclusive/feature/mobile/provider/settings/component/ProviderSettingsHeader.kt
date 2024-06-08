package com.flixclusive.feature.mobile.provider.settings.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.getProviderStatusColor
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.mobile.component.ImageWithSmallPlaceholder
import com.flixclusive.feature.mobile.provider.settings.util.getFlagFromLanguageCode
import com.flixclusive.gradle.entities.Author
import com.flixclusive.gradle.entities.Language
import com.flixclusive.gradle.entities.ProviderData
import com.flixclusive.gradle.entities.ProviderType
import com.flixclusive.gradle.entities.Status
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.util.R as UtilR

@Composable
internal fun ProviderSettingsHeader(
    modifier: Modifier = Modifier,
    providerData: ProviderData
) {
    Column(
        modifier = modifier
            .padding(top = 16.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HeaderIcon(
            providerData = providerData,
            modifier = Modifier
                .padding(bottom = 10.dp)
        )


        AuthorsList(authors = providerData.authors)
    }
}

@Composable
private fun AuthorsList(
    authors: List<Author>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            HorizontalDivider(
                thickness = 1.dp,
                color = LocalContentColor.current.onMediumEmphasis()
            )

            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 5.dp)
            ) {
                Text(
                    text = stringResource(id = UtilR.string.authors),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = LocalContentColor.current.onMediumEmphasis(),
                        fontSize = 15.sp
                    )
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(authors) {
                AuthorCard(author = it)
            }
        }

        HorizontalDivider(
            thickness = 1.dp,
            color = LocalContentColor.current.onMediumEmphasis()
        )
    }
}

@Composable
private fun HeaderIcon(
    modifier: Modifier = Modifier,
    providerData: ProviderData
) {
    val statusColor = getProviderStatusColor(providerData.status).onMediumEmphasis(0.6F)
    val contentColor = LocalContentColor.current.onMediumEmphasis(0.4F)

    val commonStyle = MaterialTheme.typography.titleMedium.copy(
        fontWeight = FontWeight.Medium,
        color = contentColor,
        fontSize = 14.sp
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        ImageWithSmallPlaceholder(
            modifier = Modifier.size(100.dp),
            placeholderModifier = Modifier.size(55.dp),
            urlImage = providerData.iconUrl,
            placeholderId = UiCommonR.drawable.provider_logo,
            contentDescId = com.flixclusive.core.util.R.string.provider_icon_content_desc,
            shape = MaterialTheme.shapes.small,
        )

        Text(
            text = providerData.name,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                fontSize = 25.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(top = 12.dp)
        )

        Text(
            modifier = Modifier
                .padding(vertical = 2.dp),
            text = buildAnnotatedString {
                withStyle(commonStyle.toSpanStyle()) {
                    append("v${providerData.versionName}")
                    append(" - ")
                    append(getFlagFromLanguageCode(providerData.language.languageCode))
                    append(" - ")
                }

                withStyle(
                    commonStyle.copy(
                        color = statusColor
                    ).toSpanStyle()
                ) {
                    append(providerData.status.toString())
                }
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Medium,
                color = contentColor,
                fontSize = 14.sp
            )
        )

        Text(
            text = providerData.providerType.toString(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Medium,
                color = contentColor,
                fontSize = 14.sp
            )
        )
    }
}


@Preview
@Composable
private fun ProviderSettingsHeaderPreview() {
    val providerData = remember {
        ProviderData(
            authors = listOf(Author("FLX")),
            repositoryUrl = null,
            buildUrl = null,
            changelog = null,
            changelogMedia = null,
            versionName = "1.0.0",
            versionCode = 10000,
            description = "lorem ipsum lorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsumlorem ipsum",
            iconUrl = null,
            language = Language.Multiple,
            name = "123Movies",
            providerType = ProviderType.All,
            status = Status.Working
        )
    }
    
    FlixclusiveTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
            ) {
                ProviderSettingsHeader(providerData = providerData)
            }
        }
    }
}