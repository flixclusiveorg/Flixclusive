package com.flixclusive.feature.mobile.searchExpanded.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.feature.mobile.searchExpanded.util.Constant
import com.flixclusive.model.provider.ProviderMetadata
import kotlin.math.max
import com.flixclusive.core.strings.R as LocaleR

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SearchProvidersView(
    providerMetadataList: List<ProviderMetadata>,
    selectedProviderId: String,
    onChangeProvider: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedIndex =
        remember {
            providerMetadataList.indexOfFirst { it.id == selectedProviderId }
        }

    val listState =
        rememberLazyListState(
            initialFirstVisibleItemIndex = max(selectedIndex, 0),
        )

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(bottom = 10.dp),
    ) {
        stickyHeader {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(bottom = 5.dp),
            ) {
                Text(
                    text = stringResource(id = LocaleR.string.get_search_results_from),
                    style =
                        MaterialTheme.typography.labelMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = LocalContentColor.current.copy(0.8F),
                        ),
                    modifier = Modifier.padding(horizontal = 10.dp),
                )
            }
        }

        item {
            val tmdbMetadata = Constant.tmdbProviderMetadata
            SearchProviderBlock(
                providerMetadata = tmdbMetadata,
                isSelected = selectedProviderId == tmdbMetadata.id,
                onClick = {
                    onChangeProvider(tmdbMetadata.id)
                },
            )
        }

        itemsIndexed(
            providerMetadataList,
            key = { _, item -> item.id },
        ) { i, item ->
            SearchProviderBlock(
                providerMetadata = item,
                isSelected = item.id == selectedProviderId,
                onClick = {
                    onChangeProvider(item.id)
                },
            )
        }
    }
}
