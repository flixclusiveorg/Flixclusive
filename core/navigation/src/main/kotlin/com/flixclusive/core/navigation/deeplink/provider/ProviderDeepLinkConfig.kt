package com.flixclusive.core.navigation.deeplink.provider

object ProviderDeepLinkConfig {
    const val PROVIDER_DEEP_LINK_BASE = "flixclusive://provider"
    const val OPEN_SETTINGS_DEEP_LINK = "$PROVIDER_DEEP_LINK_BASE/{id}/settings"

    // TODO: Add more deep links for other actions like opening provider details, etc.
}
