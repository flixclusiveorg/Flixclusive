package com.flixclusive.feature.mobile.search.component.filter

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import coil3.compose.AsyncImage
import com.flixclusive.core.presentation.common.extensions.buildImageRequest
import com.flixclusive.feature.mobile.search.R
import com.flixclusive.feature.mobile.search.SearchViewType
import com.flixclusive.feature.mobile.search.util.FilterHelper
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.core.drawables.R as UiCommonR

@Composable
internal fun ProviderFilterButton(
    currentViewType: SearchViewType,
    provider: ProviderMetadata?,
    onChangeView: (SearchViewType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var lastViewTypeSelected by rememberSaveable { mutableIntStateOf(currentViewType.ordinal) }

    OutlinedButton(
        onClick = {
            val viewType = when (currentViewType) {
                SearchViewType.Providers -> {
                    SearchViewType.entries[lastViewTypeSelected]
                }

                else -> {
                    lastViewTypeSelected = currentViewType.ordinal
                    SearchViewType.Providers
                }
            }

            onChangeView(viewType)
        },
        colors = FilterHelper.getButtonColors(isBeingUsed = true),
        border = ButtonDefaults.outlinedButtonBorder(true),
        contentPadding = PaddingValues(horizontal = 12.dp),
        shape = MaterialTheme.shapes.small,
        modifier = modifier
            .height(32.dp)
            .widthIn(min = 150.dp),
    ) {
        AnimatedContent(
            targetState = provider,
            label = "",
        ) {
            if (it == null || it.name.isEmpty()) {
                Text(
                    text = stringResource(R.string.search_provider_button_placeholder),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                )
            } else {
                NonEmptyFilterButton(
                    name = it.name,
                    iconUrl = provider?.iconUrl,
                )
            }
        }
    }
}

@Composable
private fun NonEmptyFilterButton(
    name: String,
    iconUrl: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isIconLoadingError by remember(iconUrl) { mutableStateOf(false) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        if (isIconLoadingError) {
            Icon(
                painter = painterResource(id = UiCommonR.drawable.provider_logo),
                contentDescription = name,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(12.dp),
            )
        } else {
            val imageModel = remember { context.buildImageRequest(iconUrl) }

            AsyncImage(
                model = imageModel,
                contentDescription = name,
                onError = { isIconLoadingError = true },
                modifier = Modifier
                    .size(12.dp),
            )

            Spacer(modifier = Modifier.width(3.dp))
        }

        Text(
            text = name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
        )
    }
}
