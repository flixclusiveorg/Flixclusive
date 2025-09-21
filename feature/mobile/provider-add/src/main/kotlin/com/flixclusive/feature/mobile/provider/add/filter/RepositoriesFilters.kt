package com.flixclusive.feature.mobile.provider.add.filter

import androidx.compose.ui.util.fastDistinctBy
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.strings.R
import com.flixclusive.feature.mobile.provider.add.SearchableProvider
import com.flixclusive.model.provider.Repository
import com.flixclusive.model.provider.Repository.Companion.toValidRepositoryLink
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.util.Locale

internal data class RepositoriesFilters(
    override val options: ImmutableList<String>,
    override val title: UiText,
    override val selectedValue: Set<String>,
) : AddProviderFilterType.MultiSelect() {
    companion object {
        const val REPOSITORY_NAME_OWNER_FORMAT = "%s/%s"

        fun List<SearchableProvider>.filterRepositories(filter: RepositoriesFilters): List<SearchableProvider> {
            if (filter.selectedValue.isEmpty()) return this

            return fastFilter { provider ->
                val repository = provider.metadata.repositoryUrl.toValidRepositoryLink()
                val formattedName =
                    String.format(Locale.getDefault(), REPOSITORY_NAME_OWNER_FORMAT, repository.owner, repository.name)
                filter.selectedValue.contains(formattedName)
            }
        }

        fun List<SearchableProvider>.toRepositoryFilters(repositories: List<Repository>): RepositoriesFilters {
            val repositoriesFromPreferences = repositories.fastMap { repository ->
                String.format(Locale.getDefault(), REPOSITORY_NAME_OWNER_FORMAT, repository.owner, repository.name)
            }

            val repositoriesFromProviders = fastMap {
                val repository = it.metadata.repositoryUrl.toValidRepositoryLink()
                String.format(Locale.getDefault(), REPOSITORY_NAME_OWNER_FORMAT, repository.owner, repository.name)
            }

            val options = (repositoriesFromPreferences + repositoriesFromProviders)
                .fastDistinctBy { it }
                .toImmutableList()

            return RepositoriesFilters(
                options = options,
                selectedValue = setOf(),
                title = UiText.StringResource(R.string.repositories),
            )
        }
    }
}
