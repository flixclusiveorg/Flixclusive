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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.feature.mobile.searchExpanded.SearchExpandedScreenViewModel
import com.flixclusive.feature.mobile.searchExpanded.util.Constant
import com.flixclusive.core.locale.R as LocaleR

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SearchProvidersView(
    modifier: Modifier = Modifier,
    viewModel: SearchExpandedScreenViewModel,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        safeCall {
            listState.animateScrollToItem(viewModel.selectedProviderIndex)
        }
    }

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(bottom = 10.dp)
    ) {
        stickyHeader {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(bottom = 5.dp)
            ) {
                Text(
                    text = stringResource(id = LocaleR.string.get_search_results_from),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = LocalContentColor.current.onMediumEmphasis(0.8F)
                    ),
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
            }
        }

        item {
            SearchProviderBlock(
                providerData = Constant.tmdbProviderData,
                isSelected = viewModel.selectedProviderIndex == 0,
                onClick = {
                    viewModel.onChangeProvider(0)
                }
            )
        }

        itemsIndexed(viewModel.providerDataList) { i, item ->
            SearchProviderBlock(
                providerData = item,
                isSelected = viewModel.selectedProviderIndex == i + 1,
                onClick = {
                    viewModel.onChangeProvider(i + 1)
                }
            )
        }
    }
}