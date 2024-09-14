package com.flixclusive.model.provider.link

import kotlin.reflect.KClass

/**
 * Represents a link to media content, such as a video stream or subtitle file.
 *
 * @property name The name of the media link.
 * @property url The URL of the media link.
 * @property flags A list of constraint [Flag]s associated with the media link, such as IP restrictions or expiration.
 * @property description An optional description of the media link.
 *
 * @see Flag
 */
sealed class MediaLink {
    abstract val name: String
    abstract val url: String
    abstract val flags: Set<Flag>?
    open val description: String? = null

    val customHeaders: Map<String, String>?
        get() = flags
            ?.getOrNull(Flag.RequiresAuth::class)
            ?.customHeaders

    companion object {
        /**
         *
         * Obtains a specific [Flag] type from a set of flags.
         *
         * @param flagType The type of [Flag] to obtain.
         *
         * @return The [Flag] of the specified type, or null if not found.
         * */
        fun <T : Flag> Set<Flag>.getOrNull(flagType: KClass<T>): T? {
            return this.filterIsInstance(flagType.java).firstOrNull()
        }
    }
}