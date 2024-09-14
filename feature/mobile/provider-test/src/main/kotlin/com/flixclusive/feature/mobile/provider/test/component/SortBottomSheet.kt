package com.flixclusive.feature.mobile.provider.test.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.BottomSheetDefaults.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.ui.common.R
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.feature.mobile.provider.test.SortOption
import com.flixclusive.core.locale.R as LocaleR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SortBottomSheet(
    selectedSortOption: SortOption,
    onSort: (SortOption) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.small.copy(
            bottomEnd = CornerSize(0.dp),
            bottomStart = CornerSize(0.dp)
        ),
        dragHandle = { DragHandle() }
    ) {
        LazyColumn(
            contentPadding = PaddingValues(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            items(SortOption.SortType.entries) {
                val isSelected = selectedSortOption.sort == it

                SortTypeTextButton(
                    selectedSortOption = selectedSortOption,
                    isSelected = isSelected,
                    sortType = it,
                    onSort = {
                        val sortOption =
                            if (isSelected) {
                                selectedSortOption.copy(ascending = !selectedSortOption.ascending)
                            } else selectedSortOption.copy(sort = it)

                        onSort(sortOption)
                    }
                )
            }
        }
    }
}

@Composable
private fun SortTypeTextButton(
    modifier: Modifier = Modifier,
    selectedSortOption: SortOption,
    isSelected: Boolean,
    sortType: SortOption.SortType,
    onSort: () -> Unit,
) {
    val labelStyle = MaterialTheme.typography.labelLarge.copy(
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface.onMediumEmphasis(
            if (isSelected) 1F
            else 0.7F
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 35.dp)
            .clickable { onSort() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .padding(horizontal = 10.dp)
        ) {
            Text(
                text = sortType.toString(LocalContext.current),
                style = labelStyle,
                modifier = Modifier
                    .weight(1F)
                    .align(Alignment.CenterVertically)
            )

            AnimatedContent(
                targetState = selectedSortOption,
                label = "",
                modifier = Modifier.size(16.dp)
                    .align(Alignment.CenterVertically)
            ) {
                if (isSelected) {
                    val iconId = when (it.ascending) {
                        true -> R.drawable.sort_ascending
                        else -> R.drawable.sort_descending
                    }

                    Icon(
                        painter = painterResource(id = iconId),
                        contentDescription = stringResource(id = LocaleR.string.sort_icon_content_desc),
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.CenterVertically)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun SortBottomSheetPreview() {

}