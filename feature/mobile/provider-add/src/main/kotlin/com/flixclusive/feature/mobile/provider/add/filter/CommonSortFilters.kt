package com.flixclusive.feature.mobile.provider.add.filter

import android.content.Context
import com.flixclusive.core.strings.UiText
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
)
