package com.flixclusive.feature.mobile.provider.add.filter

import androidx.compose.runtime.Immutable
import com.flixclusive.core.common.locale.UiText
import kotlinx.collections.immutable.ImmutableList

internal sealed class AddProviderFilterType<T> {
    abstract val title: UiText
    abstract val selectedValue: T

    @Immutable
    abstract class MultiSelect : AddProviderFilterType<Set<String>>() {
        abstract val options: ImmutableList<String>
    }

    @Immutable
    abstract class Sort<S>(
        val options: ImmutableList<S>
    ) : AddProviderFilterType<Sort.SortSelection>() {
        data class SortSelection(
            val index: Int,
            val ascending: Boolean = true,
        )
    }
}
