package com.flixclusive.model.provider

import com.flixclusive.gradle.entities.ProviderData

/**
 *
 * Gets the unique identifier for the provider as
 * name alone is not reliable enough to determine
 * the uniqueness of a provider.
 *
 * */
val ProviderData.id: String
    get() {
        val repositoryName = if (repositoryUrl != null) {
            "-" + getRepositoryNameFromUrl(repositoryUrl!!)
        } else ""

        return "$name$repositoryName-$versionCode"
    }

private fun getRepositoryNameFromUrl(url: String): String {
    val regex = "github\\.com/[^/]+/([^/]+)".toRegex()
    val matchResult = regex.find(url)

    return matchResult?.groups?.get(1)?.value ?: url
}