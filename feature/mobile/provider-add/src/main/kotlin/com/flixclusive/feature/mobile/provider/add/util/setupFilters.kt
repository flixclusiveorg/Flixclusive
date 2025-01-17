package com.flixclusive.feature.mobile.provider.add.util

import androidx.compose.ui.util.fastFilter
import com.flixclusive.feature.mobile.provider.add.filter.AuthorsFilters
import com.flixclusive.feature.mobile.provider.add.filter.CommonSortFilters
import com.flixclusive.feature.mobile.provider.add.filter.LanguagesFilters
import com.flixclusive.feature.mobile.provider.add.filter.ProviderTypeFilters
import com.flixclusive.feature.mobile.provider.add.filter.RepositoriesFilters
import com.flixclusive.feature.mobile.provider.add.filter.SortableProperty
import com.flixclusive.feature.mobile.provider.add.filter.StatusFilters
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.Repository.Companion.toValidRepositoryLink
import java.util.Locale

private inline fun <T : Comparable<T>> getSortComparator(
    ascending: Boolean,
    crossinline selector: (ProviderMetadata) -> T?,
): Comparator<ProviderMetadata> {
    return if (ascending) {
        compareBy(selector)
    } else {
        compareByDescending(selector)
    }
}

internal fun List<ProviderMetadata>.sort(filter: CommonSortFilters): List<ProviderMetadata> {
    val option = filter.options[filter.selectedValue.index]

    return when (option) {
        SortableProperty.Name -> {
            sortedWith(getSortComparator(filter.selectedValue.ascending) { it.name })
        }

        SortableProperty.Repository -> {
            sortedWith(
                getSortComparator(filter.selectedValue.ascending) {
                    it.repositoryUrl.toValidRepositoryLink().name
                },
            )
        }

        SortableProperty.Language -> {
            sortedWith(
                getSortComparator(filter.selectedValue.ascending) { it.language.languageCode },
            )
        }

        SortableProperty.Status -> {
            sortedWith(getSortComparator(filter.selectedValue.ascending) { it.status.ordinal })
        }
    }
}

internal fun List<ProviderMetadata>.filterAuthors(filter: AuthorsFilters): List<ProviderMetadata> {
    if (filter.selectedValue.isEmpty()) return this

    return fastFilter { provider ->
        provider.authors.any { author ->
            filter.selectedValue.contains(author.name)
        }
    }
}

internal fun List<ProviderMetadata>.filterRepositories(filter: RepositoriesFilters): List<ProviderMetadata> {
    if (filter.selectedValue.isEmpty()) return this

    return fastFilter { provider ->
        val repository = provider.repositoryUrl.toValidRepositoryLink()
        val formattedName =
            String.format(Locale.getDefault(), REPOSITORY_NAME_OWNER_FORMAT, repository.owner, repository.name)
        filter.selectedValue.contains(formattedName)
    }
}

internal fun List<ProviderMetadata>.filterLanguages(filter: LanguagesFilters): List<ProviderMetadata> {
    if (filter.selectedValue.isEmpty()) return this

    return fastFilter { provider ->
        filter.selectedValue.contains(provider.language.languageCode)
    }
}

internal fun List<ProviderMetadata>.filterProviderType(filter: ProviderTypeFilters): List<ProviderMetadata> {
    if (filter.selectedValue.isEmpty()) return this

    return fastFilter { provider ->
        filter.selectedValue.contains(provider.providerType.type)
    }
}

internal fun List<ProviderMetadata>.filterStatus(filter: StatusFilters): List<ProviderMetadata> {
    if (filter.selectedValue.isEmpty()) return this

    return fastFilter { provider ->
        filter.selectedValue.contains(provider.status.name)
    }
}
