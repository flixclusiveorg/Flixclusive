package com.flixclusive.data.backup.util

import android.content.Context
import androidx.datastore.dataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.flixclusive.core.datastore.SYSTEM_PREFS_FILENAME
import com.flixclusive.core.datastore.util.USER_PREFERENCE_FILENAME
import kotlinx.coroutines.flow.first
import java.io.File

object BackupPreferenceUtil {
    private fun Context.getUserPreferencesFile(userId: Int): File {
        return preferencesDataStoreFile("$USER_PREFERENCE_FILENAME-$userId")
    }

    private fun createTempFileForPreferences(file: File): File {
        val tempFile = File(file.parentFile, "${file.nameWithoutExtension}-tmp.json")
        file.copyTo(tempFile, overwrite = true)
        return tempFile
    }

    private suspend fun Context.getUserPreferencesFromJson(userId: Int): Map<String, String> {
        val actualFile = getUserPreferencesFile(userId)
        val tempFile = try {
            createTempFileForPreferences(actualFile)
        } catch (_: NoSuchFileException) {
            return emptyMap()
        }
        val dataStore = PreferenceDataStoreFactory.create { tempFile }
        val preferences = dataStore.data.first()

        val result = mutableMapOf<String, String>()
        preferences.asMap().forEach { (key, value) ->
            when (value) {
                is String -> result[key.name] = value
                else -> throw IllegalArgumentException(
                    "Unsupported preference type for key: ${key.name}, value: $value"
                )
            }
        }

        tempFile.delete()
        return result
    }

    private fun Context.getSystemPreferencesFromJson(): Map<String, String> {
        val systemPrefsJson = dataStoreFile(SYSTEM_PREFS_FILENAME)
        val tempFile = try {
            createTempFileForPreferences(systemPrefsJson)
        } catch (_: NoSuchFileException) {
            return emptyMap()
        }
        val jsonString = tempFile.readText()

        tempFile.delete()
        return mapOf(SYSTEM_PREFS_FILENAME to jsonString)
    }

    suspend fun Context.getLocalPreferences(userId: Int): Map<String, String> {
        val userJson = getUserPreferencesFromJson(userId)
        val systemJson = getSystemPreferencesFromJson()
        return userJson + systemJson
    }
}
