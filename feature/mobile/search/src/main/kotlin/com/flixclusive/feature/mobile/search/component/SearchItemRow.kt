package com.flixclusive.feature.mobile.search.component

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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.placeholderEffect
import com.flixclusive.core.locale.UiText
import com.flixclusive.model.provider.ProviderCatalog
import com.flixclusive.model.provider.Catalog

@Composable
internal fun SearchItemRow(
    list: List<Catalog>,
    showItemNames: Boolean,
    rowTitle: UiText,
    onClick: (Catalog) -> Unit
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
                        .padding(horizontal = 15.dp)
                        .padding(top = 15.dp)
                )

                LazyRow(
                    modifier = Modifier
                        .padding(bottom = 5.dp)
                ) {
                    items(
                        list,
                        key = { item -> item.url }
                    ) {
                        val isProviderCatalog = it is ProviderCatalog
                        val name = remember(showItemNames) {
                            if(showItemNames || isProviderCatalog) it.name else null
                        }

                        SearchItemCard(
                            label = name,
                            image = it.image,
                            isProviderCatalog = isProviderCatalog,
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
    Column {
        Spacer(
            modifier = Modifier
                .height(31.dp)
                .width(145.dp)
                .padding(horizontal = 15.dp)
                .padding(top = 15.dp)
                .placeholderEffect()
        )

        LazyRow(
            modifier = Modifier
                .padding(vertical = 10.dp)
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
}


@Preview
@Composable
private fun PlaceholderPreview() {
    FlixclusiveTheme {
        Surface {
            SearchItemRowPlaceholder()
        }
    }
}