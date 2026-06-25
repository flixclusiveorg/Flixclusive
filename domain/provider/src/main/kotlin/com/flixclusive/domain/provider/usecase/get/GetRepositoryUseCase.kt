package com.flixclusive.domain.provider.usecase.get

import com.flixclusive.model.provider.Repository

/**
 * Use case to validate and retrieve a [Repository] from a given URL.
 */
interface GetRepositoryUseCase {
    /**
     * Validates the provided URL and retrieves a [Repository] object.
     *
     * @param url The URL of the repository to validate and fetch.
     *
     * @return Contains the [Repository] if successful, or an error if the URL is invalid
     *         or the repository cannot be fetched.
     * */
    suspend operator fun invoke(url: String): Repository
}
