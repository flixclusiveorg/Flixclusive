package com.flixclusive.data.backup.util

import android.content.Context
import android.net.Uri
import com.flixclusive.core.common.provider.ProviderFile.getProvidersPath
import com.flixclusive.core.common.provider.ProviderFile.getProvidersSettingsPath
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.data.backup.model.Backup
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream

@OptIn(ExperimentalSerializationApi::class)
object BackupUtil {
    private const val ZIP_MAGIC_1 = 0x50 // 'P'
    private const val ZIP_MAGIC_2 = 0x4B // 'K'
    private const val GZIP_MAGIC_1 = 0x1F
    private const val GZIP_MAGIC_2 = 0x8B


    internal const val ZIP_ENTRY_BACKUP = "backup.pb"
    internal const val ZIP_ENTRY_PROVIDERS_PREFIX = "providers/"
    internal const val ZIP_ENTRY_PROVIDERS_SETTINGS_PREFIX = "provider-settings/"

    fun Context.decodeFromUri(uri: Uri): Backup {
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw IOException("Unable to open backup: $uri")

        return BufferedInputStream(inputStream).use { buffered ->
            buffered.mark(4)
            val header = ByteArray(4)
            val read = buffered.read(header)
            buffered.reset()

            val isZip = read >= 2 &&
                header[0] == ZIP_MAGIC_1.toByte() &&
                header[1] == ZIP_MAGIC_2.toByte()

            val isGzip = read >= 2 &&
                header[0] == GZIP_MAGIC_1.toByte() &&
                header[1] == GZIP_MAGIC_2.toByte()

            when {
                isZip -> {
                    var backupBytes: ByteArray? = null
                    ZipInputStream(buffered).use { zip ->
                        generateSequence { zip.nextEntry }
                            .filterNot { it.isDirectory }
                            .first { entry ->
                                val isBackupFile = entry.name == ZIP_ENTRY_BACKUP
                                if (isBackupFile) {
                                    backupBytes = zip.readBytes()
                                }

                                isBackupFile
                            }

                        zip.closeEntry()
                    }

                    val bytes = backupBytes ?: throw IOException("Backup archive is missing '$ZIP_ENTRY_BACKUP'")
                    ProtoBuf.decodeFromByteArray(Backup.serializer(), bytes)
                }

                isGzip -> decodeFromGzip(input = buffered)
                else -> decodeFromRaw(input = buffered)
            }
        }
    }

    internal fun Context.decodeFromUriAndRestoreFiles(
        uri: Uri,
        userId: String,
        restoreProviderFiles: Boolean,
    ): Backup {
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw IOException("Unable to open backup: $uri")

        return BufferedInputStream(inputStream).use { buffered ->
            buffered.mark(4)
            val header = ByteArray(4)
            val read = buffered.read(header)
            buffered.reset()

            val isZip = read >= 2 &&
                header[0] == ZIP_MAGIC_1.toByte() &&
                header[1] == ZIP_MAGIC_2.toByte()

            val isGzip = read >= 2 &&
                header[0] == GZIP_MAGIC_1.toByte() &&
                header[1] == GZIP_MAGIC_2.toByte()

            when {
                isZip -> decodeFromZipAndRestoreFiles(
                    userId = userId,
                    input = buffered,
                    restoreProviderFiles = restoreProviderFiles,
                )

                isGzip -> decodeFromGzip(input = buffered)
                else -> decodeFromRaw(input = buffered)
            }
        }
    }

    private fun Context.decodeFromZipAndRestoreFiles(
        userId: String,
        input: BufferedInputStream,
        restoreProviderFiles: Boolean,
    ): Backup {
        var backupBytes: ByteArray? = null
        var userProvidersFolder: File? = null
        var userProvidersSettingsFolder: File? = null

        ZipInputStream(input).use { zip ->
            generateSequence { zip.nextEntry }
                .filterNot { it.isDirectory }
                .forEach { entry ->
                    safeCall {
                        when {
                            entry.name == ZIP_ENTRY_BACKUP -> {
                                backupBytes = zip.readBytes()
                            }

                            restoreProviderFiles && entry.name.startsWith(ZIP_ENTRY_PROVIDERS_PREFIX) -> {
                                val relativePath = entry.name.removePrefix(ZIP_ENTRY_PROVIDERS_PREFIX)
                                if (relativePath.isBlank()) return@forEach

                                val providersFolder = userProvidersFolder ?: run {
                                    File(getProvidersPath(userId)).apply { mkdirs() }
                                        .also { userProvidersFolder = it }
                                }

                                val outFile = safeResolveChild(providersFolder, relativePath)
                                outFile.parentFile?.mkdirs()
                                outFile.outputStream().use { output ->
                                    zip.copyTo(output)
                                }
                            }

                            restoreProviderFiles && entry.name.startsWith(ZIP_ENTRY_PROVIDERS_SETTINGS_PREFIX) -> {
                                val relativePath = entry.name.removePrefix(ZIP_ENTRY_PROVIDERS_SETTINGS_PREFIX)
                                if (relativePath.isBlank()) return@forEach

                                val settingsFolder = userProvidersSettingsFolder ?: run {
                                    File(getProvidersSettingsPath(userId)).apply { mkdirs() }
                                        .also { userProvidersSettingsFolder = it }
                                }

                                val outFile = safeResolveChild(settingsFolder, relativePath)
                                outFile.parentFile?.mkdirs()
                                outFile.outputStream().use { output ->
                                    zip.copyTo(output)
                                }
                            }
                        }
                    }
                }

            zip.closeEntry()
        }

        val bytes = backupBytes ?: throw IOException("Backup archive is missing '$ZIP_ENTRY_BACKUP'")
        return ProtoBuf.decodeFromByteArray(Backup.serializer(), bytes)
    }

    private fun decodeFromGzip(input: BufferedInputStream): Backup {
        val bytes = GZIPInputStream(input).use { it.readBytes() }
        return ProtoBuf.decodeFromByteArray(Backup.serializer(), bytes)
    }

    private fun decodeFromRaw(input: BufferedInputStream): Backup {
        val bytes = input.readBytes()
        return ProtoBuf.decodeFromByteArray(Backup.serializer(), bytes)
    }

    private fun safeResolveChild(root: File, relativePath: String): File {
        val canonicalRoot = root.canonicalFile
        val canonicalChild = File(root, relativePath).canonicalFile

        if (!canonicalChild.path.startsWith(canonicalRoot.path + File.separator)) {
            throw IOException("Invalid zip entry path: $relativePath")
        }

        return canonicalChild
    }
}
