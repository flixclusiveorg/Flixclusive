package com.flixclusive.provider.base.plugin

/*
* Copied from `Flixclusive/plugins-gradle`
*
* */


/**
 * Represents an author entity with associated information such as name, user link, and Discord ID.
 *
 * @property name The name of the author.
 * @property userLink The optional link associated with the author's profile.
 * @property discordId The optional Discord ID of the author.
 */
data class Author(
    val name: String,
    val userLink: String? = null,
    val discordId: Long? = null,
) {
    override fun toString(): String {
        return name
    }
}

/**
 * Represents the manifest information of a plugin.
 *
 * @property pluginClassName The fully qualified class name of the plugin.
 * @property name The name of the plugin.
 * @property version The version of the plugin.
 * @property requiresResources Indicates whether the plugin requires resources from the main application/apk.
 */
data class PluginManifest(
    val pluginClassName: String,
    val name: String,
    val version: String,
    val requiresResources: Boolean,
    val updateUrl: String?,
)