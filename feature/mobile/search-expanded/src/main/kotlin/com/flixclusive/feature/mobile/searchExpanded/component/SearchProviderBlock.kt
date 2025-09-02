package com.flixclusive.feature.mobile.searchExpanded.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.DummyDataForPreview.getDummyProviderMetadata
import com.flixclusive.core.presentation.mobile.components.ImageWithSmallPlaceholder
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.core.strings.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

@Composable
internal fun SearchProviderBlock(
    modifier: Modifier = Modifier,
    providerMetadata: ProviderMetadata,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clickable(enabled = !isSelected) {
                onClick()
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            ImageWithSmallPlaceholder(
                modifier = Modifier.size(60.dp),
                placeholderSize = 30.dp,
                urlImage = providerMetadata.iconUrl,
                placeholderId = UiCommonR.drawable.provider_logo,
                contentDescId = LocaleR.string.provider_icon_content_desc,
                shape = MaterialTheme.shapes.small
            )

            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = providerMetadata.name,
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
                    text = providerMetadata.providerType?.toString() ?: stringResource(LocaleR.string.unknown_provider_type),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = LocalContentColor.current.copy(0.6f),
                        fontSize = 13.sp
                    )
                )
            }

            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = stringResource(LocaleR.string.check_indicator_content_desc),
                    modifier = Modifier.size(25.dp)
                )
            }
        }
    }
}

@Preview
@Composable
private fun ProviderCardPreview() {
    FlixclusiveTheme {
        Surface {
            SearchProviderBlock(
                providerMetadata = getDummyProviderMetadata(),
                isSelected = true,
                onClick = {}
            )
        }
    }
}
