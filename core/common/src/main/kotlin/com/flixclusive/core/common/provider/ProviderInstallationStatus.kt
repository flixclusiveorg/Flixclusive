package com.flixclusive.core.common.provider

/**
 * Represents the installation status of a provider.
 *
 * - [NotInstalled] - The provider is not installed.
 * - [Installing] - The provider is currently being installed.
 * - [Installed] - The provider is installed and up to date.
 * - [Outdated] - The provider is installed but outdated.
 * */
enum class ProviderInstallationStatus {
    NotInstalled,
    Installing,
    Installed,
    Outdated,
    ;

    val isNotInstalled: Boolean
        get() = this == NotInstalled
    val isInstalled: Boolean
        get() = this == Installed
    val isOutdated: Boolean
        get() = this == Outdated
    val isInstalling: Boolean
        get() = this == Installing
}
