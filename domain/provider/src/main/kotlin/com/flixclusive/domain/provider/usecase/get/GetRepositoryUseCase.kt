package com.flixclusive.domain.provider.usecase.get

import com.flixclusive.core.network.util.Resource
import com.flixclusive.model.provider.Repository

/**
 * Use case to validate and retrieve a [Repository] from a given URL.
 *
 * This use case takes a URL as input, validates it to be a valid repository link format,
 * and then attempts to fetch the "updater.json" file from the "builds" branch of the repository.
 * If the URL is invalid or the "updater.json" file cannot be retrieved successfully,
 * a [Resource.Failure] is returned. Otherwise, a [Resource.Success] containing the validated
 * repository object is returned.
 */
interface GetRepositoryUseCase {
    /**
     * Validates the provided URL and retrieves a [Repository] object.
     *
     * @param url The URL of the repository to validate and fetch.
     *
     * @return A [Resource] containing the [Repository] if successful, or an error if the URL is invalid
     *         or the repository cannot be fetched.
     * */
    suspend operator fun invoke(url: String): Resource<Repository>
}
