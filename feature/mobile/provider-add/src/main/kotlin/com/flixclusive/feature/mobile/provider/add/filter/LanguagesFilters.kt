package com.flixclusive.feature.mobile.provider.add.filter

import androidx.compose.ui.util.fastDistinctBy
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.strings.R
import com.flixclusive.feature.mobile.provider.add.ProviderItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal data class LanguagesFilters(
    override val options: ImmutableList<String>,
    override val title: UiText,
    override val selectedValue: Set<String>,
) : AddProviderFilterType.MultiSelect() {
    companion object {
        fun List<ProviderItem>.filterLanguages(filter: LanguagesFilters): List<ProviderItem> {
            if (filter.selectedValue.isEmpty()) return this

            return fastFilter { provider ->
                filter.selectedValue.contains(provider.metadata.language.code)
            }
        }

        fun List<ProviderItem>.toLanguageFilters(): LanguagesFilters {
            val options = fastMap { it.metadata.language.code }
                .fastDistinctBy { it }
                .toImmutableList()

            return LanguagesFilters(
                options = options,
                selectedValue = setOf(),
                title = UiText.StringResource(R.string.languages),
            )
        }
    }
}
