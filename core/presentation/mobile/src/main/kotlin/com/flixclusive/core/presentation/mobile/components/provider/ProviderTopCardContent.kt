package com.flixclusive.core.presentation.mobile.components.provider

import android.annotation.SuppressLint
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
import com.flixclusive.core.presentation.mobile.R
import com.flixclusive.core.presentation.mobile.components.ImageWithSmallPlaceholder
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@SuppressLint("ModifierParameter")
@Suppress("compose:modifier-naming")
@Composable
fun ProviderTopCardContent(
    providerMetadata: ProviderMetadata,
    isDraggable: Boolean,
    dragModifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AnimatedContent(
            targetState = isDraggable,
            label = "",
            modifier = dragModifier,
        ) { state ->
            if (state) {
                Icon(
                    painter = painterResource(id = R.drawable.round_drag_indicator_24),
                    contentDescription =
                        stringResource(
                            id = LocaleR.string.drag_icon_content_desc,
                        ),
                    modifier =
                        Modifier
                            .size(30.dp),
                )
            }
        }

        ImageWithSmallPlaceholder(
            modifier = Modifier.size(60.dp),
            placeholderSize = 30.dp,
            urlImage = providerMetadata.iconUrl,
            placeholderId = UiCommonR.drawable.provider_logo,
            contentDescId = LocaleR.string.provider_icon_content_desc,
            shape = MaterialTheme.shapes.small,
        )

        ProviderDetails(
            providerMetadata = providerMetadata,
            modifier =
                Modifier
                    .weight(1F)
                    .padding(start = 5.dp),
        )
    }
}

@Composable
private fun ProviderDetails(
    providerMetadata: ProviderMetadata,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth(),
        ) {
            Text(
                text = providerMetadata.name,
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                    ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier
                        .padding(bottom = 2.dp),
            )

            Text(
                text = "v${providerMetadata.versionName}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Normal,
                        color = LocalContentColor.current.copy(0.4F),
                        fontSize = 13.sp,
                    ),
            )
        }

        val authors =
            remember {
                if (providerMetadata.authors.size == 1) {
                    context.getString(
                        LocaleR.string.made_by_author_label_format,
                        providerMetadata.authors.firstOrNull()?.name ?: "anon",
                    )
                } else {
                    providerMetadata.authors
                        .map { it.name }
                        .take(3)
                        .joinToString(", ")
                }
            }

        Text(
            text = authors,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style =
                MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Light,
                    color = LocalContentColor.current.copy(0.6f),
                    fontSize = 13.sp,
                ),
        )

        Text(
            text = providerMetadata.providerType.toString(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style =
                MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = LocalContentColor.current.copy(0.6f),
                    fontSize = 13.sp,
                ),
        )
    }
}
