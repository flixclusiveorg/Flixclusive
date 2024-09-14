package com.flixclusive.model.provider.link

/**
 * Represents a flag associated with a URL.
 *
 * @see IPLocked
 * @see Expires
 * @see RequiresAuth
 * @see Trusted
 */
sealed class Flag {
    /**
     * Indicates that the URL is locked to a specific IP address.
     */
    data object IPLocked : Flag()

    /**
     * Indicates that the URL expires at a specific time.
     *
     * @param expiresOn The timestamp (in milliseconds) when the URL expires.
     */
    data class Expires(val expiresOn: Long) : Flag()

    /**
     * Indicates that the URL requires authentication.
     *
     * @param customHeaders Optional custom headers to include in the authentication request.
     */
    data class RequiresAuth(val customHeaders: Map<String, String>?) : Flag()

    /**
     * Indicates that the media link comes from a trusted and reputable provider.
     *
     * @property name The name of the trusted provider, e.g., "Netflix", "Amazon Prime".
     * @property logo URL or resource ID of the provider's logo or icon.
     * @property description A brief description or tagline of the provider.
     * @property rating A rating or score for the provider.
     * @property url A URL to the provider's official website or page.
     * @property category The category of the provider, e.g., "Streaming Service".
     * @property contact Contact information for support or inquiries related to the provider.
     */
    data class Trusted(
        val name: String,
        val logo: String? = null,
        val description: String? = null,
        val rating: Double? = null,
        val url: String? = null,
        val category: String? = null,
        val contact: String? = null
    ) : Flag() {
        companion object {
            private val trustedProviders = listOf(
                "Netflix",
                "Amazon Prime Video",
                "Hulu",
                "Disney+",
                "HBO Max",
                "Apple TV+",
                "Paramount+",
                "Peacock",
                "YouTube Premium",
                "BBC iPlayer",
                "Hulu + Live TV",
                "Starz",
                "Showtime",
                "Sundance Now",
                "Discovery+",
                "Crunchyroll",
                "FuboTV",
                "Vudu",
                "Tubi",
                "Acorn TV"
            )

            fun Trusted.isTrusted(name: String) {
                if (!trustedProviders.contains(name)) {
                    throw IllegalArgumentException("Invalid trusted provider: $name")
                }
            }
        }
    }
}

