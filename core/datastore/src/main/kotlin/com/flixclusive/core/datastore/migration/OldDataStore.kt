package com.flixclusive.core.datastore.migration

import android.content.Context
import androidx.datastore.dataStore
import com.flixclusive.core.datastore.migration.serializer.AppSettingsProviderSerializer
import com.flixclusive.core.datastore.migration.serializer.AppSettingsSerializer
import com.flixclusive.core.datastore.migration.serializer.OnBoardingPreferencesSerializer

internal const val OLD_APP_SETTINGS_FILENAME = "app-preferences.json"
internal val Context.oldAppSettings by dataStore(
    OLD_APP_SETTINGS_FILENAME, AppSettingsSerializer,
)

internal const val OLD_APP_PROVIDER_SETTINGS_FILENAME = "app-provider-preferences.json"
internal val Context.oldAppProviderSettings by dataStore(
    OLD_APP_PROVIDER_SETTINGS_FILENAME, AppSettingsProviderSerializer
)

internal const val OLD_ON_BOARDING_PREFS_FILENAME = "on-boarding-preferences.json"
internal val Context.oldOnBoardingPreferences by dataStore(
    OLD_ON_BOARDING_PREFS_FILENAME, OnBoardingPreferencesSerializer
)