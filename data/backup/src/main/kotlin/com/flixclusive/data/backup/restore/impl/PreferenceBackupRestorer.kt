package com.flixclusive.data.backup.restore.impl

import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.SYSTEM_PREFS_FILENAME
import com.flixclusive.core.datastore.model.system.SystemPreferences
import com.flixclusive.core.datastore.model.user.DataPreferences
import com.flixclusive.core.datastore.model.user.PlayerPreferences
import com.flixclusive.core.datastore.model.user.ProviderPreferences
import com.flixclusive.core.datastore.model.user.SubtitlesPreferences
import com.flixclusive.core.datastore.model.user.UiPreferences
import com.flixclusive.core.datastore.model.user.UserOnBoarding
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.data.backup.model.BackupPreference
import com.flixclusive.data.backup.model.StringPreferenceValue
import com.flixclusive.data.backup.restore.BackupRestorer
import kotlinx.serialization.json.Json
import javax.inject.Inject

internal class PreferenceBackupRestorer @Inject constructor(
    private val dataStoreManager: DataStoreManager,
) : BackupRestorer<BackupPreference> {
    override suspend fun invoke(items: List<BackupPreference>): Result<Unit> {
        return runCatching {
            restoreSystemPreferences(items)
            restoreUserPreferences(items)
        }
    }

    private suspend fun restoreSystemPreferences(items: List<BackupPreference>) {
        val map = items.associateBy { it.key }
        val restored = Json.decodeFromString<SystemPreferences>(
            map[SYSTEM_PREFS_FILENAME]?.asStringOrNull() ?: return
        )

        dataStoreManager.updateSystemPrefs { restored }
    }

    private suspend fun restoreUserPreferences(items: List<BackupPreference>) {
        val map = items.associateBy { it.key }

        map[UserPreferences.DATA_PREFS_KEY.name].asStringOrNull()?.let { json ->
            val restored = Json.decodeFromString<DataPreferences>(json)
            dataStoreManager.updateUserPrefs(UserPreferences.DATA_PREFS_KEY, DataPreferences::class) { restored }
        }

        map[UserPreferences.PLAYER_PREFS_KEY.name].asStringOrNull()?.let { json ->
            val restored = Json.decodeFromString<PlayerPreferences>(json)
            dataStoreManager.updateUserPrefs(UserPreferences.PLAYER_PREFS_KEY, PlayerPreferences::class) { restored }
        }

        map[UserPreferences.PROVIDER_PREFS_KEY.name].asStringOrNull()?.let { json ->
            val restored = Json.decodeFromString<ProviderPreferences>(json)
            dataStoreManager.updateUserPrefs(UserPreferences.PROVIDER_PREFS_KEY, ProviderPreferences::class) { restored }
        }

        map[UserPreferences.SUBTITLES_PREFS_KEY.name].asStringOrNull()?.let { json ->
            val restored = Json.decodeFromString<SubtitlesPreferences>(json)
            dataStoreManager.updateUserPrefs(UserPreferences.SUBTITLES_PREFS_KEY, SubtitlesPreferences::class) { restored }
        }

        map[UserPreferences.UI_PREFS_KEY.name].asStringOrNull()?.let { json ->
            val restored = Json.decodeFromString<UiPreferences>(json)
            dataStoreManager.updateUserPrefs(UserPreferences.UI_PREFS_KEY, UiPreferences::class) { restored }
        }

        map[UserPreferences.USER_ON_BOARDING_PREFS_KEY.name].asStringOrNull()?.let { json ->
            val restored = Json.decodeFromString<UserOnBoarding>(json)
            dataStoreManager.updateUserPrefs(UserPreferences.USER_ON_BOARDING_PREFS_KEY, UserOnBoarding::class) { restored }
        }
    }

    private fun BackupPreference?.asStringOrNull(): String? {
        return (this?.value as? StringPreferenceValue)?.value
    }

}
