package com.flixclusive.feature.mobile.provider.component


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.feature.mobile.provider.R
import com.flixclusive.gradle.entities.ProviderData
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.util.R as UtilR


@Composable
internal fun TopCardContent(
    providerData: ProviderData,
    isSearching: Boolean
) {
    val context = LocalContext.current

    var errorLoadingIcon by remember { mutableStateOf(false) }
    val iconImage = remember {
        ImageRequest.Builder(context)
            .data(providerData.iconUrl)
            .build()
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .padding(vertical = 10.dp)
    ) {
        if (!isSearching) {
            Icon(
                painter = painterResource(id = R.drawable.round_drag_indicator_24),
                contentDescription = stringResource(
                    id = UtilR.string.drag_icon_content_desc
                ),
                modifier = Modifier
                    .size(30.dp)
            )
        }

        Surface(
            shape = MaterialTheme.shapes.small,
            tonalElevation = 80.dp
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = iconImage,
                    imageLoader = LocalContext.current.imageLoader,
                    placeholder = painterResource(id = UiCommonR.drawable.provider_logo),
                    onError = { errorLoadingIcon = true },
                    contentDescription = stringResource(UtilR.string.provider_icon_content_desc),
                    alignment = Alignment.Center,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(40.dp)
                )
            }
        }

        ProviderDetails(
            providerData = providerData,
            modifier = Modifier
                .weight(1F)
                .padding(start = 5.dp)
        )
    }
}

@Composable
private fun ProviderDetails(
    providerData: ProviderData,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = providerData.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(bottom = 2.dp)
            )


            Text(
                text = "v${providerData.versionName}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Normal,
                    color = LocalContentColor.current.onMediumEmphasis(0.4F),
                    fontSize = 13.sp
                )
            )
        }

        val authors = remember {
            if (providerData.authors.size == 1) {
                context.getString(
                    UtilR.string.made_by_author_label_format,
                    providerData.authors.firstOrNull()?.name ?: "anon"
                )
            } else {
                providerData.authors.take(3).joinToString(", ")
            }
        }

        Text(
            text = authors,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Light,
                color = LocalContentColor.current.onMediumEmphasis(),
                fontSize = 13.sp
            )
        )

        Text(
            text = providerData.providerType?.toString() ?: stringResource(UtilR.string.unknown_provider_type),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                color = LocalContentColor.current.onMediumEmphasis(),
                fontSize = 13.sp
            )
        )
    }
}