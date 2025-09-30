package com.flixclusive.feature.mobile.search.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.presentation.common.components.FilmCover
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.components.Placeholder
import com.flixclusive.feature.mobile.search.util.SearchUiUtils
import com.flixclusive.model.provider.Catalog

@Composable
internal fun DiscoverRow(
    list: List<Catalog>,
    rowTitle: UiText,
    itemContent: @Composable LazyItemScope.(catalog: Catalog) -> Unit,
) {
    Column {
        Text(
            text = rowTitle.asString(),
            style = MaterialTheme.typography.labelLarge.asAdaptiveTextStyle(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 15.dp, bottom = 10.dp),
        )

        LazyRow(
            modifier = Modifier.padding(bottom = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(
                list,
                key = { item -> item.url },
            ) {
                itemContent(it)
            }

            if (list.isEmpty()) {
                items(10) {
                    Placeholder(
                        modifier = Modifier
                            .width(SearchUiUtils.getCardWidth())
                            .aspectRatio(FilmCover.Backdrop.ratio),
                    )
                }
            }
        }
    }
}
