package com.flixclusive.feature.mobile.provider.add.filter

import android.content.Context
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.strings.R
import com.flixclusive.feature.mobile.provider.add.SearchableProvider
import com.flixclusive.model.provider.Repository.Companion.toValidRepositoryLink
import kotlinx.collections.immutable.toImmutableList
import com.flixclusive.core.strings.R as LocaleR

internal enum class SortableProperty(
    val uiText: UiText,
) {
    Name(UiText.StringResource(LocaleR.string.name)),
    Repository(UiText.StringResource(LocaleR.string.repository)),
    Language(UiText.StringResource(LocaleR.string.language)),
    Status(UiText.StringResource(LocaleR.string.status));

    fun toString(context: Context): String {
        return uiText.asString(context)
    }
}

internal data class CommonSortFilters(
    override val title: UiText,
    override val selectedValue: SortSelection,
) : AddProviderFilterType.Sort<SortableProperty>(
    options = SortableProperty.entries.toImmutableList(),
) {
    companion object {
        private inline fun <T : Comparable<T>> getSortComparator(
            ascending: Boolean,
            crossinline selector: (SearchableProvider) -> T?,
        ): Comparator<SearchableProvider> {
            return if (ascending) {
                compareBy(selector)
            } else {
                compareByDescending(selector)
            }
        }

        fun List<SearchableProvider>.sort(filter: CommonSortFilters): List<SearchableProvider> {
            val option = filter.options[filter.selectedValue.index]

            return when (option) {
                SortableProperty.Name -> {
                    sortedWith(getSortComparator(filter.selectedValue.ascending) { it.metadata.name })
                }

                SortableProperty.Repository -> {
                    sortedWith(
                        getSortComparator(filter.selectedValue.ascending) {
                            it.metadata.repositoryUrl
                                .toValidRepositoryLink()
                                .name
                        },
                    )
                }

                SortableProperty.Language -> {
                    sortedWith(
                        getSortComparator(filter.selectedValue.ascending) { it.metadata.language.languageCode },
                    )
                }

                SortableProperty.Status -> {
                    sortedWith(getSortComparator(filter.selectedValue.ascending) { it.metadata.status.ordinal })
                }
            }
        }

        fun create(): CommonSortFilters {
            return CommonSortFilters(
                selectedValue = SortSelection(0),
                title = UiText.StringResource(R.string.sort_by),
            )
        }
    }
}
