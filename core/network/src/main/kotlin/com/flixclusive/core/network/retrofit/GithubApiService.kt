package com.flixclusive.core.network.retrofit

import com.flixclusive.core.util.common.configuration.GITHUB_REPOSITORY
import com.flixclusive.core.util.common.configuration.GITHUB_USERNAME
import com.flixclusive.model.configuration.GithubLastCommit
import com.flixclusive.model.configuration.GithubReleaseNotes
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
     * @return A [GithubLastCommit] object.
     */
    @GET("repos/$GITHUB_USERNAME/$GITHUB_REPOSITORY/git/refs/heads/{branch}")
    suspend fun getLastCommitObject(
        @Path("branch") branch: String = "master"
    ): GithubLastCommit

    /**
     * Retrieves the release notes for the given tag.
     *
     * @param tag The tag name.
     * @return A [GithubReleaseNotes] object.
     */
    @GET("repos/$GITHUB_USERNAME/$GITHUB_REPOSITORY/releases/tags/{tag}")
    suspend fun getReleaseNotes(
        @Path("tag") tag: String
    ): GithubReleaseNotes
}