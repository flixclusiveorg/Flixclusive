package com.flixclusive.core.datastore

import android.content.Context
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.model.datastore.user.ProviderFromPreferences
import java.io.File
import java.nio.charset.StandardCharsets

private const val NEW_PROVIDERS_FOLDER_NAME = "providers"
private const val OLD_PROVIDERS_FOLDER_NAME = "flx_providers"
private const val PROVIDERS_SETTINGS_FOLDER_NAME = "settings"

internal object ProviderFromPreferencesMigration {
    fun migrateToNewPaths(
        oldProviders: List<ProviderFromPreferences>,
        context: Context,
    ): List<ProviderFromPreferences> {
        if (oldProviders.isEmpty()) return oldProviders

        context.migrateProviders()
        context.migrateProviderSettings()
        return context.getNewPaths(oldProviders = oldProviders)
    }

    private fun Context.getNewPaths(oldProviders: List<ProviderFromPreferences>): List<ProviderFromPreferences> {
        val newFolder = getNewMigrationFolder(NEW_PROVIDERS_FOLDER_NAME)
        return oldProviders.mapNotNull {
            val oldFile = File(it.filePath)
            val oldParentFolder =
                oldFile.parentFile
                    ?: return@mapNotNull null

            val newFolderName =
                getNewRepositoryFolderFromOldName(oldParentFolder.name)
                    ?: return@mapNotNull null

            it.copy(
                filePath =
                    File(newFolder, newFolderName)
                        .resolve(oldFile.name)
                        .absolutePath,
            )
        }
    }

    private fun Context.migrateProviders() {
        val oldFolder = getOldProvidersFolder()
        if (!oldFolder.exists()) return

        val newFolder = getNewMigrationFolder(NEW_PROVIDERS_FOLDER_NAME)

        oldFolder.listFiles()?.forEach { repositoryFolder ->
            if (!repositoryFolder.isDirectory) return@forEach

            val folderName =
                getNewRepositoryFolderFromOldName(repositoryFolder.name)
                    ?: return@forEach

            val newLocation = File(newFolder, folderName)
            moveDirectory(repositoryFolder, newLocation)
        }

        oldFolder.delete()
    }

    private fun Context.migrateProviderSettings() {
        val oldFolder = getOldProviderSettingsFolder()
        if (!oldFolder.exists()) return

        val newFolder = getNewMigrationFolder(PROVIDERS_SETTINGS_FOLDER_NAME)

        oldFolder.listFiles()?.forEach { repositoryFolder ->
            if (!repositoryFolder.isDirectory) return@forEach

            val folderName =
                getNewRepositoryFolderFromOldName(repositoryFolder.name)
                    ?: return@forEach

            val newLocation = File(newFolder, folderName)
            moveDirectory(repositoryFolder, newLocation)
        }
    }

    private fun moveDirectory(
        source: File,
        destination: File,
    ) {
        if (!destination.exists()) {
            destination.mkdirs()
        }

        source.listFiles()?.forEach { file ->
            val newFile = File(destination, file.name)

            if (file.isDirectory) {
                moveDirectory(file, newFile)
            } else {
                if (!file.renameTo(newFile)) {
                    errorLog("Failed to move file: ${file.absolutePath}")
                    file.copyTo(newFile, overwrite = true)
                    file.delete()
                }
            }
        }

        if (!source.delete()) {
            errorLog("Failed to delete original directory: ${source.absolutePath}")
        }
    }

    private fun getNewRepositoryFolderFromOldName(folderName: String): String? {
        val pattern = """https___github\.com_([a-zA-Z0-9-]+)_([a-zA-Z0-9-]+)""".toRegex()

        return pattern.find(folderName)?.let { matchResult ->
            val username = matchResult.groupValues[1]
            val repository = matchResult.groupValues[2]

            "$username-$repository".toValidFilename()
        }
    }

    private fun Context.getNewMigrationFolder(rootFolder: String): File =
        getExternalFilesDir(null)!!
            .resolve(rootFolder)
            .resolve("user-1")

    private fun Context.getOldProvidersFolder(): File = filesDir.resolve(OLD_PROVIDERS_FOLDER_NAME)

    private fun Context.getOldProviderSettingsFolder(): File =
        getExternalFilesDir(null)!!
            .resolve(PROVIDERS_SETTINGS_FOLDER_NAME)

    /**
     * Mutate the given filename to make it valid for a FAT filesystem,
     * replacing any invalid characters with "_".
     *
     */
    internal fun String.toValidFilename(): String {
        if (isEmpty() || this == "." || this == "..") {
            return "(invalid)"
        }

        val res = StringBuilder(length)
        for (c in this) {
            if (isValidFilenameChar(c)) {
                res.append(c)
            } else {
                res.append('_')
            }
        }

        trimFilename(res)
        return res.toString()
    }

    private fun isValidFilenameChar(c: Char): Boolean {
        val charByte = c.code.toByte()
        // Control characters (0x00 to 0x1F) are not allowed
        if (charByte in 0x00..0x1F) {
            return false
        }
        // Specific characters are also not allowed
        if (c.code == 0x7F) {
            return false
        }

        return when (c) {
            '"', '*', '/', ':', '<', '>', '?', '\\', '|' -> false
            else -> true
        }
    }

    private fun trimFilename(
        res: StringBuilder,
        maxBytes: Int = 254,
    ) {
        var rawBytes = res.toString().toByteArray(StandardCharsets.UTF_8)
        if (rawBytes.size > maxBytes) {
            // Reduce max bytes to account for "..."
            val adjustedMaxBytes = maxBytes - 3
            while (rawBytes.size > adjustedMaxBytes) {
                // Remove character from the middle
                res.deleteCharAt(res.length / 2)
                rawBytes = res.toString().toByteArray(StandardCharsets.UTF_8)
            }
            // Insert "..." in the middle
            res.insert(res.length / 2, "...")
        }
    }
}
