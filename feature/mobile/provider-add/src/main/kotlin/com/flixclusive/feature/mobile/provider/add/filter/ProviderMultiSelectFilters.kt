package com.flixclusive.feature.mobile.provider.add.filter

import com.flixclusive.core.locale.UiText
import kotlinx.collections.immutable.ImmutableList

internal data class AuthorsFilters(
    override val options: ImmutableList<String>,
    override val title: UiText,
    override val selectedValue: Set<String>,
) : AddProviderFilterType.MultiSelect()


internal data class LanguagesFilters(
    override val options: ImmutableList<String>,
    override val title: UiText,
    override val selectedValue: Set<String>,
) : AddProviderFilterType.MultiSelect()


internal data class RepositoriesFilters(
    override val options: ImmutableList<String>,
    override val title: UiText,
    override val selectedValue: Set<String>,
) : AddProviderFilterType.MultiSelect()


internal data class ProviderTypeFilters(
    override val options: ImmutableList<String>,
    override val title: UiText,
    override val selectedValue: Set<String>,
) : AddProviderFilterType.MultiSelect()


internal data class StatusFilters(
    override val options: ImmutableList<String>,
    override val title: UiText,
    override val selectedValue: Set<String>,
) : AddProviderFilterType.MultiSelect()
