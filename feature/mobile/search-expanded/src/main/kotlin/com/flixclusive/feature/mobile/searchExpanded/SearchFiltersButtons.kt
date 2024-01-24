package com.flixclusive.feature.mobile.searchExpanded

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flixclusive.core.ui.common.SearchFilter
import com.flixclusive.core.ui.common.util.onMediumEmphasis

@Composable
internal fun SearchFiltersButtons(
    currentFilterSelected: SearchFilter,
    onFilterChange: (SearchFilter) -> Unit,
) {
    val selectedButtonColors = ButtonDefaults.outlinedButtonColors(
        disabledContainerColor = MaterialTheme.colorScheme.primary,
        disabledContentColor = MaterialTheme.colorScheme.onPrimary
    )
    val unselectedButtonColors = ButtonDefaults.outlinedButtonColors(contentColor = LocalContentColor.current.onMediumEmphasis())

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp)
    ) {
        SearchFilter.entries.forEach { filter ->
            val buttonColors = remember(currentFilterSelected) {
                when (currentFilterSelected == filter) {
                    true -> selectedButtonColors
                    false -> unselectedButtonColors
                }
            }

            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .padding(vertical = 12.dp)
            ) {
                OutlinedButton(
                    onClick = { onFilterChange(filter) },
                    enabled = currentFilterSelected != filter,
                    colors = buttonColors,
                    contentPadding = PaddingValues(horizontal = 12.dp),
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