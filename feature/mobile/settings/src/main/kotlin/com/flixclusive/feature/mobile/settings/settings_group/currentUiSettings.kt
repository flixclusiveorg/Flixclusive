package com.flixclusive.feature.mobile.settings.settings_group

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import com.flixclusive.feature.mobile.settings.SettingsItem
import com.flixclusive.feature.mobile.settings.util.AppSettingsHelper.rememberLocalAppSettings
import com.flixclusive.feature.mobile.settings.util.AppSettingsHelper.rememberAppSettingsChanger
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun currentUiSettings(): List<SettingsItem> {
    val appSettings by rememberLocalAppSettings()
    val onChangeSettings by rememberAppSettingsChanger()

    return listOf(
        SettingsItem(
            title = stringResource(LocaleR.string.film_card_titles),
            description = stringResource(LocaleR.string.film_card_titles_label),
            onClick = {
                onChangeSettings(appSettings.copy(isShowingFilmCardTitle = !appSettings.isShowingFilmCardTitle))
            },
            previewContent = {
                Switch(
                    checked = appSettings.isShowingFilmCardTitle,
                    onCheckedChange = {
                        onChangeSettings(appSettings.copy(isShowingFilmCardTitle = it))
                    },
                    modifier = Modifier.scale(0.7F)
                )
            },
        )
    )
}