package com.flixclusive.presentation.mobile.screens.search.expanded_content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flixclusive.presentation.mobile.main.ITEMS_START_PADDING
import com.flixclusive.presentation.mobile.main.LABEL_START_PADDING
import com.flixclusive.presentation.mobile.screens.search.common.SearchFilter
import com.flixclusive.presentation.mobile.utils.ComposeMobileUtils.colorOnMediumEmphasisMobile

@Composable
fun SearchFiltersButtons(
    currentFilterSelected: SearchFilter,
    onFilterChange: (SearchFilter) -> Unit,
) {
    val selectedButtonColors = ButtonDefaults.outlinedButtonColors(
        disabledContainerColor = MaterialTheme.colorScheme.primary,
        disabledContentColor = MaterialTheme.colorScheme.onPrimary
    )
    val unselectedButtonColors = ButtonDefaults.outlinedButtonColors(contentColor = colorOnMediumEmphasisMobile())

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = LABEL_START_PADDING)
    ) {
        SearchFilter.values().forEach { filter ->
            val buttonColors = remember(currentFilterSelected) {
                when (currentFilterSelected == filter) {
                    true -> selectedButtonColors
                    false -> unselectedButtonColors
                }
            }

            Box(
                modifier = Modifier
                    .padding(start = ITEMS_START_PADDING)
                    .padding(vertical = ITEMS_START_PADDING)
            ) {
                OutlinedButton(
                    onClick = { onFilterChange(filter) },
                    enabled = currentFilterSelected != filter,
                    colors = buttonColors,
                    contentPadding = PaddingValues(horizontal = ITEMS_START_PADDING),
                    modifier = Modifier
                        .height(35.dp)
                ) {
                    Text(
                        text = stringResource(filter.resId),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}