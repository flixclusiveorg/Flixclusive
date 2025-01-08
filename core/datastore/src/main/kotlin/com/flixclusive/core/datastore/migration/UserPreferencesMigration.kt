package com.flixclusive.core.datastore.migration

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.dataStoreFile
import androidx.datastore.preferences.core.Preferences
import com.flixclusive.core.datastore.ProviderFromPreferencesMigration.migrateToNewPaths
import com.flixclusive.core.datastore.migration.model.OldAppSettings
import com.flixclusive.core.datastore.migration.model.OldAppSettingsProvider
import com.flixclusive.core.datastore.migration.model.OldOnBoardingPreferences
import com.flixclusive.model.datastore.user.DataPreferences
import com.flixclusive.model.datastore.user.PlayerPreferences
import com.flixclusive.model.datastore.user.ProviderPreferences
import com.flixclusive.model.datastore.user.SubtitlesPreferences
import com.flixclusive.model.datastore.user.UiPreferences
import com.flixclusive.model.datastore.user.UserOnBoarding
import com.flixclusive.model.datastore.user.UserPreferences
import com.flixclusive.model.datastore.user.player.CaptionSizePreference.Companion.getDp
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class UserPreferencesMigration(
    private val context: Context,
) : DataMigration<Preferences> {
    private val oldDataStoreFile =
        context.dataStoreFile(OLD_APP_SETTINGS_FILENAME)
    private val oldOnBoardingPreferencesFile =
        context.dataStoreFile(OLD_ON_BOARDING_PREFS_FILENAME)
    private val oldAppSettingsProviderFile =
        context.dataStoreFile(OLD_APP_PROVIDER_SETTINGS_FILENAME)

    override suspend fun cleanUp() {
        oldDataStoreFile.delete()
        oldOnBoardingPreferencesFile.delete()
        oldAppSettingsProviderFile.delete()
    }

    override suspend fun migrate(currentData: Preferences): Preferences {
        val oldAppSettings = context.oldAppSettings.data.first()
        val oldAppProviderSettings = context.oldAppProviderSettings.data.first()
        val oldOnBoardingPreferences = context.oldOnBoardingPreferences.data.first()

        val uiPreferences = oldAppSettings.toUiPreferences()
        val providerPreferences = oldAppProviderSettings.toProviderPreferences(context)
        val userOnBoarding = oldOnBoardingPreferences.toUserOnBoarding()
        val dataPreferences = oldAppSettings.toDataPreferences()
        val subtitlesPreferences = oldAppSettings.toSubtitlesPreferences()
        val playerPreferences = oldAppSettings.toPlayerPreferences()

        val currentMutablePrefs = currentData.toMutablePreferences()

        currentMutablePrefs[UserPreferences.UI_PREFS_KEY] =
            Json.encodeToString(uiPreferences)
        currentMutablePrefs[UserPreferences.PROVIDER_PREFS_KEY] =
            Json.encodeToString(providerPreferences)
        currentMutablePrefs[UserPreferences.USER_ON_BOARDING_PREFS_KEY] =
            Json.encodeToString(userOnBoarding)
        currentMutablePrefs[UserPreferences.DATA_PREFS_KEY] =
            Json.encodeToString(dataPreferences)
        currentMutablePrefs[UserPreferences.SUBTITLES_PREFS_KEY] =
            Json.encodeToString(subtitlesPreferences)
        currentMutablePrefs[UserPreferences.PLAYER_PREFS_KEY] =
            Json.encodeToString(playerPreferences)

        return currentMutablePrefs.toPreferences()
    }

    override suspend fun shouldMigrate(currentData: Preferences): Boolean =
        oldDataStoreFile.exists() ||
            oldAppSettingsProviderFile.exists() ||
            oldOnBoardingPreferencesFile.exists()

    private fun OldAppSettings.toPlayerPreferences(): PlayerPreferences =
        PlayerPreferences(
            isForcingPlayerRelease = shouldReleasePlayer,
            isDurationReversed = isPlayerTimeReversed,
            isPiPModeEnabled = isPiPModeEnabled,
            isUsingVolumeBoost = isUsingVolumeBoost,
            audioLanguage = preferredAudioLanguage,
            bufferCacheSize = preferredBufferCacheSize,
            resizeMode = preferredResizeMode,
            diskCacheSize = preferredDiskCacheSize,
            videoBufferMs = preferredVideoBufferMs,
            seekAmount = preferredSeekAmount,
            quality = preferredQuality,
            decoderPriority = decoderPriority,
        )

    private fun OldAppSettings.toSubtitlesPreferences(): SubtitlesPreferences =
        SubtitlesPreferences(
            isSubtitleEnabled = isSubtitleEnabled,
            subtitleLanguage = subtitleLanguage,
            subtitleColor = subtitleColor,
            subtitleSize = subtitleSize.getDp(),
            subtitleFontStyle = subtitleFontStyle,
            subtitleBackgroundColor = subtitleBackgroundColor,
            subtitleEdgeType = subtitleEdgeType,
        )

    private fun OldAppSettings.toDataPreferences(): DataPreferences = DataPreferences(isIncognito = isIncognito)

    private fun OldAppSettings.toUiPreferences(): UiPreferences = UiPreferences(shouldShowTitleOnCards = isShowingFilmCardTitle)

    private fun OldAppSettingsProvider.toProviderPreferences(context: Context): ProviderPreferences {
        val providersWithNewFilePaths =
            migrateToNewPaths(
                oldProviders = providers,
                context = context,
            )

        return ProviderPreferences(
            shouldWarnBeforeInstall = warnOnInstall,
            isAutoUpdateEnabled = isUsingAutoUpdateProviderFeature,
            repositories = repositories,
            providers = providersWithNewFilePaths,
        )
    }

    private fun OldOnBoardingPreferences.toUserOnBoarding(): UserOnBoarding =
        UserOnBoarding(
            isFirstTimeOnProvidersScreen = isFirstTimeOnProvidersScreen,
        )
}
