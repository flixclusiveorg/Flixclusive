package com.flixclusive.feature.mobile.provider.add.filter

import androidx.compose.ui.util.fastDistinctBy
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastFlatMap
import androidx.compose.ui.util.fastMap
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.strings.R
import com.flixclusive.feature.mobile.provider.add.ProviderItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal data class AuthorsFilters(
    override val options: ImmutableList<String>,
    override val title: UiText,
    override val selectedValue: Set<String>,
) : AddProviderFilterType.MultiSelect() {
    companion object {
        fun List<ProviderItem>.filterAuthors(filter: AuthorsFilters): List<ProviderItem> {
            if (filter.selectedValue.isEmpty()) return this

            return fastFilter { provider ->
                provider.metadata.authors.any { author ->
                    filter.selectedValue.contains(author.name)
                }
            }
        }

        fun List<ProviderItem>.toAuthorFilters(): AuthorsFilters {
            val options = fastFlatMap { option ->
                option.metadata.authors.fastMap { it.name }
            }.fastDistinctBy { it }
                .toImmutableList()

            return AuthorsFilters(
                options = options,
                selectedValue = setOf(),
                title = UiText.StringResource(R.string.authors),
            )
        }
    }
}
