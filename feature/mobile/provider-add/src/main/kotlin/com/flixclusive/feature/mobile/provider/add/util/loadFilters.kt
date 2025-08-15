package com.flixclusive.feature.mobile.provider.add.util

import androidx.compose.ui.util.fastDistinctBy
import androidx.compose.ui.util.fastFlatMap
import androidx.compose.ui.util.fastMap
import com.flixclusive.core.strings.UiText
import com.flixclusive.feature.mobile.provider.add.filter.AddProviderFilterType
import com.flixclusive.feature.mobile.provider.add.filter.AuthorsFilters
import com.flixclusive.feature.mobile.provider.add.filter.CommonSortFilters
import com.flixclusive.feature.mobile.provider.add.filter.LanguagesFilters
import com.flixclusive.feature.mobile.provider.add.filter.ProviderTypeFilters
import com.flixclusive.feature.mobile.provider.add.filter.RepositoriesFilters
import com.flixclusive.feature.mobile.provider.add.filter.StatusFilters
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.Repository
import com.flixclusive.model.provider.Repository.Companion.toValidRepositoryLink
import kotlinx.collections.immutable.toImmutableList
import java.util.Locale
import com.flixclusive.core.strings.R as LocaleR

internal const val REPOSITORY_NAME_OWNER_FORMAT = "%s/%s"

internal fun loadSortFilters(): CommonSortFilters {
    return CommonSortFilters(
        selectedValue = AddProviderFilterType.Sort.SortSelection(0),
        title = UiText.StringResource(LocaleR.string.sort_by),
    )
}

internal fun List<ProviderMetadata>.loadAuthorFilters(): AuthorsFilters {
    val mapped =
        fastFlatMap {
            it.authors.fastMap { it.name }
        }

    val options = mutableListOf<String>()
    options.addAll(mapped)
    val possibleOptions = options.fastDistinctBy { it }.toImmutableList()

    return AuthorsFilters(
        options = possibleOptions,
        selectedValue = setOf(),
        title = UiText.StringResource(LocaleR.string.authors),
    )
}

internal fun List<ProviderMetadata>.loadLanguageFilters(): LanguagesFilters {
    val mapped = fastMap { it.language.languageCode }

    val options = mutableListOf<String>()
    options.addAll(mapped)
    val possibleOptions = options.fastDistinctBy { it }.toImmutableList()

    return LanguagesFilters(
        options = possibleOptions,
        selectedValue = setOf(),
        title = UiText.StringResource(LocaleR.string.languages),
    )
}

internal fun List<ProviderMetadata>.loadRepositoryFilters(repositories: List<Repository>): RepositoriesFilters {
    val repositoriesFromPreferences =
        repositories
            .fastMap { repository ->
                String.format(Locale.getDefault(), REPOSITORY_NAME_OWNER_FORMAT, repository.owner, repository.name)
            }
    val repositoriesFromProviders =
        fastMap {
            val repository = it.repositoryUrl.toValidRepositoryLink()
            String.format(Locale.getDefault(), REPOSITORY_NAME_OWNER_FORMAT, repository.owner, repository.name)
        }

    val repositoryOptions = mutableListOf<String>()
    repositoryOptions.addAll(repositoriesFromPreferences)
    repositoryOptions.addAll(repositoriesFromProviders)
    val possibleOptions = repositoryOptions.fastDistinctBy { it }.toImmutableList()

    return RepositoriesFilters(
        options = possibleOptions,
        selectedValue = setOf(),
        title = UiText.StringResource(LocaleR.string.repositories),
    )
}

internal fun List<ProviderMetadata>.loadProviderTypeFilters(): ProviderTypeFilters {
    val mapped = fastMap { it.providerType.type }

    val options = mutableListOf<String>()
    options.addAll(mapped)
    val possibleOptions = options.fastDistinctBy { it }.toImmutableList()

    return ProviderTypeFilters(
        options = possibleOptions,
        selectedValue = setOf(),
        title = UiText.StringResource(LocaleR.string.provider_types),
    )
}

internal fun List<ProviderMetadata>.loadStatusFilters(): StatusFilters {
    val mapped = fastMap { it.status.name } // TODO: Try making this resource dependent

    val options = mutableListOf<String>()
    options.addAll(mapped)
    val possibleOptions = options.fastDistinctBy { it }.toImmutableList()

    return StatusFilters(
        options = possibleOptions,
        selectedValue = setOf(),
        title = UiText.StringResource(LocaleR.string.status),
    )
}

// TODO("Add an adult filter")
