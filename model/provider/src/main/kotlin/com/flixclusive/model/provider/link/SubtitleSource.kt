package com.flixclusive.model.provider.link

/**
 * Enum class representing different types or sources of subtitle content.
 *
 * This enum class defines the possible sources for subtitle file, such as online subtitles,
 * locally stored files, or embedded content.
 *
 * @property ONLINE Represents subtitle file sourced from an online url.
 * @property LOCAL Represents subtitle file stored locally.
 * @property EMBEDDED Represents an embedded subtitle file, usually found in `mkv` formats.
 */
enum class SubtitleSource {
    ONLINE,
    LOCAL,
    EMBEDDED
}