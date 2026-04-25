package com.flixclusive.feature.mobile.search.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.common.provider.getProviderStatusContainerColor
import com.flixclusive.core.presentation.common.extensions.buildImageRequest
import com.flixclusive.core.presentation.common.util.DummyDataForPreview.getProviderMetadata
import com.flixclusive.core.presentation.mobile.components.ImageWithSmallPlaceholder
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.feature.mobile.search.SearchProvider
import com.flixclusive.model.provider.ProviderStatus
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun SearchProviderBlock(
    provider: SearchProvider,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .graphicsLayer {
                alpha = if (provider.isEnabled) 1F else 0.5F
            }
            .clickable(enabled = !isSelected && provider.isEnabled) {
                onClick()
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp),
        ) {
            ImageWithSmallPlaceholder(
                model = remember { context.buildImageRequest(provider.iconUrl) },
                placeholder = painterResource(UiCommonR.drawable.provider_logo),
                contentDescription = provider.name,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(40.dp),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = provider.name,
                    style = MaterialTheme.typography.labelMedium
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "v${provider.versionName} (${provider.versionCode})",
                        style = MaterialTheme.typography.labelSmall,
                        color = LocalContentColor.current.copy(0.6F)
                    )


                    if (provider.status != ProviderStatus.Working) {
                        Text(
                            text = provider.status.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = getProviderStatusContainerColor(provider.status),
                            modifier = Modifier
                                .graphicsLayer { alpha = 0.6F }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Icon(
                    painter = painterResource(UiCommonR.drawable.check),
                    contentDescription = stringResource(LocaleR.string.check_indicator_content_desc),
                    modifier = Modifier.size(18.dp)
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
                provider = SearchProvider(getProviderMetadata(), true),
                isSelected = true,
                onClick = {}
            )
        }
    }
}
