package com.flixclusive.model.provider

/**
 * Represents the status of a provider.
 *
 * @see Down
 * @see Maintenance
 * @see Beta
 * @see Working
 */
enum class Status {
    /** Indicates that the provider is currently down. */
    Down,

    /** Indicates that the provider is under maintenance. */
    Maintenance,

    /** Indicates that the provider is in beta testing. */
    Beta,

    /** Indicates that the provider is working without issues. */
    Working
}