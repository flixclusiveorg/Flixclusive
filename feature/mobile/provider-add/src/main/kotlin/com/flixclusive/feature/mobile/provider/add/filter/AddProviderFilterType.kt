package com.flixclusive.feature.mobile.provider.add.filter

import com.flixclusive.core.strings.UiText
import kotlinx.collections.immutable.ImmutableList

internal sealed class AddProviderFilterType<T> {
    abstract val title: UiText
    abstract val selectedValue: T

    abstract class MultiSelect : AddProviderFilterType<Set<String>>() {
        abstract val options: ImmutableList<String>
    }

    abstract class Sort<S>(
        val options: ImmutableList<S>
    ) : AddProviderFilterType<Sort.SortSelection>() {
        data class SortSelection(
            val index: Int,
            val ascending: Boolean = true,
        )
    }
}
