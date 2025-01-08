package com.flixclusive.data.provider.util

import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.model.provider.ProviderManifest
import com.flixclusive.provider.Provider
import com.flixclusive.provider.settings.ProviderSettings
import dalvik.system.PathClassLoader
import java.io.File
import java.io.InputStreamReader

internal inline fun <reified T> PathClassLoader.getFileFromPath(file: String): T {
    val manifest: T

    getResourceAsStream(file).use { stream ->
        if (stream == null) {
            throw NullPointerException("No $file found")
        }

        InputStreamReader(stream).use { reader ->
            manifest = fromJson(reader)
        }
    }

    return manifest
}

@Suppress("UNCHECKED_CAST")
internal fun PathClassLoader.getProviderInstance(
    file: File,
    settingsDirPath: String,
    manifest: ProviderManifest,
): Provider {
    val providerClass: Class<out Provider?> =
        loadClass(manifest.providerClassName) as Class<out Provider>

    val provider = providerClass.getDeclaredConstructor().newInstance() as Provider

    provider.__filename = file.name
    provider.manifest = manifest
    provider.settings =
        ProviderSettings(
            fileDirectory = settingsDirPath,
            providerId = manifest.id,
        )

    return provider
}
