package com.flixclusive.core.network.retrofit

import com.flixclusive.core.network.retrofit.dto.GithubRelease
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * API Interface for interacting with the GitHub API.
 */
interface GithubApiService {
    /**
     * Retrieves the latest stable release for the given tag.
     *
     * @return A [GithubRelease] object.
     */
    @GET("repos/{owner}/{repository}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repository") repository: String,
    ): GithubRelease
}
