package com.flixclusive.presentation.mobile.screens.search.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flixclusive.common.UiText
import com.flixclusive.domain.model.config.SearchCategoryItem
import com.flixclusive.presentation.mobile.main.LABEL_START_PADDING
import com.flixclusive.presentation.utils.ModifierUtils.placeholderEffect

@Composable
fun SearchItemRow(
    list: List<SearchCategoryItem>,
    showItemNames: Boolean,
    rowTitle: UiText,
    onClick: (SearchCategoryItem) -> Unit
) {
    Box {
        AnimatedVisibility(
            visible = list.isEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            SearchItemRowPlaceholder()
        }

        AnimatedVisibility(
            visible = list.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column {
                Text(
                    text = rowTitle.asString(),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LABEL_START_PADDING)
                        .padding(top = LABEL_START_PADDING)
                )

                LazyRow(
                    modifier = Modifier
                        .padding(bottom = 5.dp)
                ) {
                    items(
                        items = list,
                        key = { it.name }
                    ) {
                        SearchItemCard(
                            label = if(showItemNames) it.name else null,
                            posterPath = it.posterPath,
                            onClick = {
                                onClick(it)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchItemRowPlaceholder() {
    Spacer(
        modifier = Modifier
            .height(31.dp)
            .width(145.dp)
            .padding(horizontal = LABEL_START_PADDING)
            .padding(top = LABEL_START_PADDING)
            .placeholderEffect()
    )

    LazyRow(
        modifier = Modifier
            .padding(bottom = 10.dp)
    ) {
        items(5) {
            Spacer(
                modifier = Modifier
                    .height(88.dp)
                    .width(190.dp)
                    .padding(10.dp)
                    .placeholderEffect()
            )
        }
    }
}