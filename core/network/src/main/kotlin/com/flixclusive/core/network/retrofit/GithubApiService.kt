package com.flixclusive.core.network.retrofit

import com.flixclusive.core.util.common.GithubConstant.GITHUB_REPOSITORY
import com.flixclusive.core.util.common.GithubConstant.GITHUB_USERNAME
import com.flixclusive.model.configuration.GithubBranchInfo
import com.flixclusive.model.configuration.GithubReleaseInfo
import com.flixclusive.model.configuration.GithubTagInfo
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * API Interface for interacting with the GitHub API.
 */
interface GithubApiService {
    /**
     * Retrieves the last commit object for the given branch.
     *
     * @param branch The branch name. Defaults to "master".
     * @return A [GithubBranchInfo] object.
     */
    @GET("repos/$GITHUB_USERNAME/$GITHUB_REPOSITORY/git/refs/heads/{branch}")
    suspend fun getLastCommitObject(
        @Path("branch") branch: String = "master"
    ): GithubBranchInfo

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
    @GET("repos/$GITHUB_USERNAME/$GITHUB_REPOSITORY/tags")
    suspend fun getTagsInfo(): List<GithubTagInfo>
}