package com.flixclusive.feature.mobile.plugin.component


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.flixclusive.feature.mobile.plugin.R
import com.flixclusive.gradle.entities.PluginData
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.util.R as UtilR


@Composable
internal fun TopCardContent(
    pluginData: PluginData
) {
    val context = LocalContext.current

    var errorLoadingIcon by remember { mutableStateOf(false) }
    val iconImage = remember {
        ImageRequest.Builder(context)
            .data(pluginData.iconUrl)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .padding(vertical = 10.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.round_drag_indicator_24),
            contentDescription = stringResource(
                id = UtilR.string.drag_icon_content_desc
            ),
            modifier = Modifier
                .size(30.dp)
        )

        Surface(
            shape = MaterialTheme.shapes.small,
            tonalElevation = 80.dp
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp),
                contentAlignment = Alignment.Center
            ) {
                if (pluginData.iconUrl != null && !errorLoadingIcon) {
                    AsyncImage(
                        model = iconImage,
                        imageLoader = LocalContext.current.imageLoader,
                        onError = {
                            errorLoadingIcon = true
                        },
                        contentDescription = stringResource(
                            UtilR.string.plugin_icon_content_desc,
                        ),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        painter = painterResource(id = UiCommonR.drawable.plugin_logo),
                        contentDescription = stringResource(id = UtilR.string.plugin_icon_content_desc),
                        tint = LocalContentColor.current.onMediumEmphasis(),
                        modifier = Modifier
                            .size(40.dp)
                    )
                }
            }
        }

        ProviderDetails(
            pluginData = pluginData,
            modifier = Modifier
                .weight(1F)
                .padding(start = 5.dp)
        )
    }
}

@Composable
private fun ProviderDetails(
    pluginData: PluginData,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
    ) {
        Text(
            text = pluginData.name,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(bottom = 2.dp)
        )

        val authors = remember {
            if (pluginData.authors.size == 1) {
                context.getString(
                    UtilR.string.made_by_author_label_format,
                    pluginData.authors.firstOrNull() ?: "anon"
                )
            } else {
                pluginData.authors.take(3).joinToString(", ")
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
            text = pluginData.pluginType.toString(),
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