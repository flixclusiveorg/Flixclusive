package com.flixclusive.core.ui.mobile.component.provider


import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.mobile.R
import com.flixclusive.core.ui.mobile.component.ImageWithSmallPlaceholder
import com.flixclusive.model.provider.ProviderData
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.locale.R as LocaleR


@Composable
internal fun TopCardContent(
    providerData: ProviderData,
    isDraggable: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnimatedContent(
            targetState = isDraggable,
            label = ""
        ) { state ->
            if (state) {
                Icon(
                    painter = painterResource(id = R.drawable.round_drag_indicator_24),
                    contentDescription = stringResource(
                        id = LocaleR.string.drag_icon_content_desc
                    ),
                    modifier = Modifier
                        .size(30.dp)
                )
            }
        }

        ImageWithSmallPlaceholder(
            modifier = Modifier.size(60.dp),
            placeholderModifier = Modifier.size(30.dp),
            urlImage = providerData.iconUrl,
            placeholderId = UiCommonR.drawable.provider_logo,
            contentDescId = LocaleR.string.provider_icon_content_desc,
            shape = MaterialTheme.shapes.small
        )

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
                    LocaleR.string.made_by_author_label_format,
                    providerData.authors.firstOrNull()?.name ?: "anon"
                )
            } else {
                providerData.authors
                    .map { it.name }
                    .take(3).joinToString(", ")
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
            text = providerData.providerType?.toString() ?: stringResource(LocaleR.string.unknown_provider_type),
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