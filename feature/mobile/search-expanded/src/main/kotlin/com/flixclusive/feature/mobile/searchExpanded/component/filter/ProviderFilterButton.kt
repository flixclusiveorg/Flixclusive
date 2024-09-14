package com.flixclusive.feature.mobile.searchExpanded.component.filter

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.flixclusive.core.ui.common.util.buildImageUrl
import com.flixclusive.feature.mobile.searchExpanded.SearchItemViewType
import com.flixclusive.feature.mobile.searchExpanded.util.FilterHelper
import com.flixclusive.feature.mobile.searchExpanded.util.FilterHelper.getButtonColors
import com.flixclusive.model.provider.ProviderData
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun ProviderFilterButton(
    modifier: Modifier = Modifier,
    currentViewType: MutableState<SearchItemViewType>,
    providerData: ProviderData
) {
    val context = LocalContext.current

    var lastViewTypeSelected by rememberSaveable { mutableStateOf(currentViewType.value.ordinal) }
    var isIconLoadingError by remember(providerData.iconUrl) { mutableStateOf(false) }

    OutlinedButton(
        onClick = {
            currentViewType.value = when (currentViewType.value) {
                SearchItemViewType.Providers -> SearchItemViewType.entries[lastViewTypeSelected]
                else -> {
                    lastViewTypeSelected = currentViewType.value.ordinal
                    SearchItemViewType.Providers
                }
            }
        },
        colors = getButtonColors(isBeingUsed = true),
        border = FilterHelper.getButtonBorders(isBeingUsed = true),
        contentPadding = PaddingValues(horizontal = 12.dp),
        shape = MaterialTheme.shapes.small,
        modifier = modifier
            .height(32.dp)
            .widthIn(min = 150.dp)
    ) {
        AnimatedContent(
            targetState = providerData.name,
            label = "",
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                if (isIconLoadingError) {
                    Icon(
                        painter = painterResource(id = UiCommonR.drawable.provider_logo),
                        contentDescription = stringResource(LocaleR.string.provider_icon_content_desc),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(16.dp)
                    )
                } else {
                    val imageModel = remember { context.buildImageUrl(providerData.iconUrl) }

                    AsyncImage(
                        model = imageModel,
                        contentDescription = stringResource(LocaleR.string.provider_icon_content_desc),
                        onError = { isIconLoadingError = true },
                        modifier = Modifier
                            .size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(3.dp))
                }

                Text(
                    text = it,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }
    }
}