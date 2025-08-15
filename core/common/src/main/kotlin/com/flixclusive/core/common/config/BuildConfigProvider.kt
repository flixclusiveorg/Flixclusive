package com.flixclusive.core.common.config

/**
 * An interface to provide build configuration details for non-app modules.
 * This is used to access build configuration properties without directly depending on the Android BuildConfig.
 * It allows for a more modular and testable design, especially in shared libraries or modules that
 *
 * @see CustomBuildConfig
 * */
interface BuildConfigProvider {
    /**
     * @return an instance of [CustomBuildConfig] containing the build configuration details.
     * */
    fun get(): CustomBuildConfig
}
