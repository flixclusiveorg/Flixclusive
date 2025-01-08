package com.flixclusive.data.provider.util

import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import android.os.Build
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.provider.Provider
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * A utility class for dynamically loading resources from an external file.
 * It handles both legacy (Marshmallow and below) and modern Android versions.
 *
 * If the device is running an Android version below Marshmallow, it copies the
 * input file to a temporary file and appends a manifest. The said step is essential
 * since Android 6< the `addAssetPath` method requires a path (a directory, file or ZIP)
 * to have a manifest file or else it will assume that the path does not have any resources.
 *
 * An old commit code from [AOSP](https://android.googlesource.com/platform/frameworks/base/+/435acfc88917e3535462ea520b01d0868266acd2/libs/androidfw/AssetManager.cpp) (Android Open Source Project):
 * ```java
 *     // Check that the path has an AndroidManifest.xml
 *     Asset* manifestAsset = const_cast<AssetManager*>(this)->openNonAssetInPathLocked(...);
 *     if (manifestAsset == NULL) {
 *         // This asset path does not contain any resources.
 *         // ...
 *         return false;
 *     }
 * ```
 *
 * This is inspired from this [stackoverflow post](https://stackoverflow.com/questions/7483568/dynamic-resource-loading-from-other-apk)
 *
 * @property context The Android application context.
 */
@Suppress("DEPRECATION")
internal class DynamicResourceLoader(
    private val context: Context,
) {
    /**
     * Loads the resources from the input file and sets them to the provided [Provider].
     * For Android Marshmallow and below, it manipulates the ZIP file before loading.
     *
     * @param inputFile The input file containing the resources to be loaded.
     * @param provider The Provider instance to set the loaded resources.
     */
    fun load(inputFile: File): Resources {
        var filePath = inputFile.absolutePath
        if (isAPI23OrBelow()) {
            val tempFile = createTempFile(inputFile)
            val manifestFile = createManifestFile(tempFile)
            manipulateZipFile(
                inputFile = inputFile,
                tempFile = tempFile,
                manifestFile = manifestFile,
            )
            filePath = tempFile.absolutePath
        }

        return getDynamicResources(filePath = filePath)
    }

    /**
     * Checks if the current Android version is Marshmallow (API 23) or below.
     *
     * @return True if the device is running Android Marshmallow or below, false otherwise.
     */
    internal fun isAPI23OrBelow(): Boolean = Build.VERSION.SDK_INT <= Build.VERSION_CODES.M

    /**
     * Creates a new Resources instance using the provided file path.
     *
     * @param filePath The path to the file containing the resources.
     * @return A new Resources instance loaded with the assets from the provided file.
     */
    private fun getDynamicResources(filePath: String): Resources {
        val assets = AssetManager::class.java.getDeclaredConstructor().newInstance()
        val addAssetPath = AssetManager::class.java.getMethod("addAssetPath", String::class.java)
        addAssetPath.invoke(assets, filePath)

        return Resources(
            assets,
            context.resources.displayMetrics,
            context.resources.configuration,
        )
    }

    /**
     * Manipulates the ZIP file for legacy Android versions.
     * This involves copying the input to a temp file and appending a manifest.
     *
     * @param inputFile The original input file.
     * @param tempFile The temporary file for manipulation.
     * @param manifestFile The manifest file to be added.
     */
    private fun manipulateZipFile(
        inputFile: File,
        tempFile: File,
        manifestFile: File,
    ) {
        copyInputToTemp(
            inputFile = inputFile,
            tempFile = tempFile,
        )
        appendManifestToZip(
            tempFile = tempFile,
            manifestFile = manifestFile,
        )
        infoLog("ZIP file manipulation completed for legacy Android version.")
    }

    /**
     * Cleans up temporary files created during the ZIP manipulation process.
     *
     * @param inputFile The original input file.
     */
    internal fun cleanupArtifacts(inputFile: File) {
        val tempFile = createTempFile(inputFile)
        val manifestFile = createManifestFile(tempFile)
        tempFile.delete()
        manifestFile.delete()
        infoLog("Cleanup completed.")
    }

    /**
     * Creates a temporary file for ZIP manipulation.
     *
     * @param inputFile The original input file.
     * @return A File object representing the temporary ZIP file.
     */
    private fun createTempFile(inputFile: File): File = File(inputFile.parent, "${inputFile.nameWithoutExtension}_temp.flx")

    /**
     * Creates a basic AndroidManifest.xml file.
     *
     * @param tempFile The temporary file, used to determine the parent directory.
     * @return A File object representing the created AndroidManifest.xml.
     */
    private fun createManifestFile(tempFile: File): File {
        val manifestContent =
            """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
            </manifest>
            """.trimIndent()
        val file = File(tempFile.parent, "AndroidManifest.xml")
        file.writeText(manifestContent)
        return file
    }

    /**
     * Copies the input file to the temporary file.
     *
     * @param inputFile The original input file.
     * @param tempFile The temporary file to copy to.
     */
    private fun copyInputToTemp(
        inputFile: File,
        tempFile: File,
    ) {
        inputFile.copyTo(tempFile, overwrite = true)
    }

    /**
     * Appends the AndroidManifest.xml to the temporary ZIP file.
     *
     * @param tempFile The temporary ZIP file.
     * @param manifestFile The manifest file to be added.
     */
    private fun appendManifestToZip(
        tempFile: File,
        manifestFile: File,
    ) {
        ZipFile(tempFile).use { zipFile ->
            val tempOutputFile = File(tempFile.parent, "${tempFile.nameWithoutExtension}_new.flx")
            ZipOutputStream(tempOutputFile.outputStream()).use { zipOutputStream ->
                copyExistingEntries(zipFile, zipOutputStream)
                addManifestToZip(zipOutputStream, manifestFile)
            }
            replaceOriginalTempFile(tempOutputFile, tempFile)
        }
    }

    /**
     * Copies existing entries from the original ZIP file to the new ZIP file.
     *
     * @param zipFile The original ZIP file.
     * @param zipOutputStream The output stream of the new ZIP file.
     */
    private fun copyExistingEntries(
        zipFile: ZipFile,
        zipOutputStream: ZipOutputStream,
    ) {
        for (entry in zipFile.entries()) {
            zipOutputStream.putNextEntry(ZipEntry(entry.name))
            zipFile.getInputStream(entry).use { input ->
                input.copyTo(zipOutputStream)
            }
            zipOutputStream.closeEntry()
        }
    }

    /**
     * Adds the AndroidManifest.xml to the ZIP file.
     *
     * @param zipOutputStream The output stream of the ZIP file.
     * @param manifestFile The manifest file to be added.
     */
    private fun addManifestToZip(
        zipOutputStream: ZipOutputStream,
        manifestFile: File,
    ) {
        zipOutputStream.putNextEntry(ZipEntry("AndroidManifest.xml"))
        manifestFile.inputStream().use { input ->
            input.copyTo(zipOutputStream)
        }
        zipOutputStream.closeEntry()
    }

    /**
     * Replaces the original temporary file with the new one.
     *
     * @param tempOutputFile The new temporary file to replace the original.
     * @param tempFile The original temporary file to be replaced.
     */
    private fun replaceOriginalTempFile(
        tempOutputFile: File,
        tempFile: File,
    ) {
        tempOutputFile.renameTo(tempFile)
    }
}
