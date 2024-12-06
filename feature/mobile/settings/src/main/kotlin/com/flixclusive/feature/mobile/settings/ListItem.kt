package com.flixclusive.feature.mobile.settings

import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

enum class ListItem(
    val iconId: Int,
    val labelId: Int
) {
    // Application Menu Items
    GENERAL_SETTINGS(
        iconId = UiCommonR.drawable.general_settings,
        labelId = LocaleR.string.general
    ),
    PROVIDERS(
        iconId = UiCommonR.drawable.provider_logo,
        labelId = LocaleR.string.providers
    ),
    APPEARANCE(
        iconId = UiCommonR.drawable.appearance_settings,
        labelId = LocaleR.string.appearance
    ),
    PLAYER(
        iconId = UiCommonR.drawable.play_outline_circle,
        labelId = LocaleR.string.player
    ),
    DATA_AND_BACKUP(
        iconId = UiCommonR.drawable.database_icon_thin,
        labelId = LocaleR.string.data_and_backup
    ),
    ADVANCED(
        iconId = UiCommonR.drawable.code,
        labelId = LocaleR.string.advanced
    ),

    // GitHub Menu Items
    ISSUE_A_BUG(
        iconId = UiCommonR.drawable.test_thin,
        labelId = LocaleR.string.issue_a_bug
    ),
    FEATURE_REQUEST(
        iconId = UiCommonR.drawable.feature_request,
        labelId = LocaleR.string.feature_request
    ),
    REPOSITORY(
        iconId = UiCommonR.drawable.github_outline,
        labelId = LocaleR.string.repository
    )
}