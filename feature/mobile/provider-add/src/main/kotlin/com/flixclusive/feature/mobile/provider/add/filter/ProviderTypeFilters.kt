package com.flixclusive.feature.mobile.provider.add.filter

import androidx.compose.ui.util.fastDistinctBy
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.strings.R
import com.flixclusive.feature.mobile.provider.add.SearchableProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal data class ProviderTypeFilters(
    override val options: ImmutableList<String>,
    override val title: UiText,
    override val selectedValue: Set<String>,
) : AddProviderFilterType.MultiSelect() {
    companion object {
        fun List<SearchableProvider>.filterProviderType(filter: ProviderTypeFilters): List<SearchableProvider> {
            if (filter.selectedValue.isEmpty()) return this

            return fastFilter { provider ->
                filter.selectedValue.contains(provider.metadata.providerType.type)
            }
        }

        fun List<SearchableProvider>.toProviderTypeFilters(): ProviderTypeFilters {
            val options = fastMap { it.metadata.providerType.type }
                .fastDistinctBy { it }
                .toImmutableList()

            return ProviderTypeFilters(
                options = options,
                selectedValue = setOf(),
                title = UiText.StringResource(R.string.provider_types),
            )
        }
    }
}
