package com.flixclusive.core.network.retrofit

import com.flixclusive.core.network.retrofit.dto.GithubReleaseInfo
import com.flixclusive.core.util.common.GithubConstant.GITHUB_REPOSITORY
import com.flixclusive.core.util.common.GithubConstant.GITHUB_USERNAME
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * API Interface for interacting with the GitHub API.
 */
interface GithubApiService {

    /**
     * Retrieves the release for the given tag.
     *
     * @param tag The tag name.
     * @return A [GithubReleaseInfo] object.
     */
    @GET("repos/$GITHUB_USERNAME/$GITHUB_REPOSITORY/releases/tags/{tag}")
    suspend fun getReleaseInfo(
        @Path("tag") tag: String
    ): GithubReleaseInfo

    /**
     * Retrieves the latest stable release for the given tag.
     *
     * @return A [GithubReleaseInfo] object.
     */
    @GET("repos/$GITHUB_USERNAME/$GITHUB_REPOSITORY/releases/latest")
    suspend fun getStableReleaseInfo(): GithubReleaseInfo

    /**
     * Retrieves the release notes for the given tag.
     *
     * @return A [GithubReleaseInfo] object.
     */
    @GET("repos/$GITHUB_USERNAME/$GITHUB_REPOSITORY/releases")
    suspend fun getReleases(): List<GithubReleaseInfo>
}
