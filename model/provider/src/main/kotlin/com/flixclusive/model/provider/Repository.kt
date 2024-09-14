package com.flixclusive.model.provider

import kotlinx.serialization.Serializable

/**
 * Represents a repository.
 * All credits to Cloudstream for the code references.
 *
 * @param owner The username or organization name.
 * @param name The repository name.
 * @param url The URL of the repository.
 * @param rawLinkFormat The raw link format used for generating raw links to files in the repository.
 */
@Serializable
data class Repository(
    val owner: String,
    val name: String,
    val url: String,
    val rawLinkFormat: String
) {
    /**
     * Generates a raw link to a file in the repository.
     *
     * @param filename The name of the file.
     * @param branch The branch name.
     * @return The raw link to the file.
     */
    fun getRawLink(filename: String, branch: String): String {
        return rawLinkFormat
            .replace("%filename%", filename)
            .replace("%branch%", branch)
    }

    companion object {
        /**
         * Creates a Repository instance based on the provided parameters.
         *
         * @param owner The username or organization name.
         * @param name The repository name.
         * @param type The type of repository (e.g., "github", "gitlab").
         * @return The Repository instance.
         */
        private fun parseRepository(
            owner: String,
            name: String,
            type: String
        ): Repository {
            return when {
                type == "github" -> Repository(
                    owner = owner,
                    name = name,
                    url = "https://github.com/${owner}/${name}",
                    rawLinkFormat = "https://raw.githubusercontent.com/${owner}/${name}/%branch%/%filename%"
                )
                type == "gitlab" -> Repository(
                    owner = owner,
                    name = name,
                    url = "https://gitlab.com/${owner}/${name}",
                    rawLinkFormat = "https://gitlab.com/${owner}/${name}/-/raw/%branch%/%filename%"
                )
                type == "codeberg" -> Repository(
                    owner = owner,
                    name = name,
                    url = "https://codeberg.org/${owner}/${name}",
                    rawLinkFormat = "https://codeberg.org/${owner}/${name}/raw/branch/%branch%/%filename%"
                )
                type.startsWith("gitlab-") -> {
                    val domain = type.removePrefix("gitlab-")
                    Repository(
                        owner = owner,
                        name = name,
                        url = "https://${domain}/${owner}/${name}",
                        rawLinkFormat = "https://${domain}/${owner}/${name}/-/raw/%branch%/%filename%"
                    )
                }
                type.startsWith("gitea-") -> {
                    val domain = type.removePrefix("gitea-")
                    Repository(
                        owner = owner,
                        name = name,
                        url = "https://${domain}/${owner}/${name}",
                        rawLinkFormat = "https://${domain}/${owner}/${name}/raw/branch/%branch%/%filename%"
                    )
                }
                else -> throw IllegalArgumentException("Unknown type ${type}. Use github, gitlab, gitlab-<domain>, or gitea-<domain> or set repository via setRepository(user, repo, url, rawLinkFormat)")
            }
        }

        /**
         * Converts a string representation of a repository link into a Repository object.
         *
         * @return The Repository instance.
         */
        fun String.toValidRepositoryLink(): Repository {
            val type: String?

            val split = when {
                startsWith("https://github.com") -> {
                    type = "github"

                    this.removePrefix("https://")
                        .removePrefix("github.com")
                }
                startsWith("https://gitlab.com") -> {
                    type = "gitlab"

                    this.removePrefix("https://")
                        .removePrefix("gitlab.com")
                }
                startsWith("https://codeberg.org") -> {
                    type = "codeberg"

                    this.removePrefix("https://")
                        .removePrefix("codeberg.org")
                }
                !startsWith("https://") -> { // assume default as github
                    type = "github"
                    this
                }
                else -> throw IllegalArgumentException("Unknown domain, please set repository via setRepository(user, repo, type)")
            }
                .removePrefix("/")
                .removeSuffix("/")
                .split("/")

            return parseRepository(split[0], split[1], type)
        }
    }
}