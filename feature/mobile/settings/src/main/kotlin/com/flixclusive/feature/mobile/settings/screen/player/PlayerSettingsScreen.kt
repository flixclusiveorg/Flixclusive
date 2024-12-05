package com.flixclusive.feature.mobile.settings.screen.player

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.flixclusive.core.ui.common.R
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.feature.mobile.settings.SettingsItem
import com.flixclusive.feature.mobile.settings.component.BaseSubScreen
import com.flixclusive.feature.mobile.settings.component.SettingsGroup
import com.flixclusive.feature.mobile.settings.screen.player.items.playerGeneralItems
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.LocalDialogKeyMap
import com.flixclusive.feature.mobile.settings.util.UiUtil.toggle
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun PlayerSettingsScreen(
    cacheLinksSize: Int,
    clearCacheLinks: () -> Unit,
) {
    val dialogKeyMap = LocalDialogKeyMap.current
    val onItemClick = fun (item: SettingsItem) {
        when {
            item.onClick != null -> item.onClick.invoke()
            else -> dialogKeyMap.toggle(item.dialogKey!!)
        }
    }

    BaseSubScreen(
        title = stringResource(LocaleR.string.player),
        description = stringResource(LocaleR.string.player_settings_content_desc)
    ) {
        item {
            SettingsGroup(
                items = playerGeneralItems(
                    cacheLinksSize = cacheLinksSize,
                    clearCacheLinks = clearCacheLinks
                ),
                onItemClick = onItemClick
            )
        }

        item {
            SettingsGroup(
                items = listOf(
                    SettingsItem(
                        title = stringResource(LocaleR.string.clear_cache_links),
                        description = stringResource(LocaleR.string.cache_links_item_count, cacheLinksSize),
                        onClick = clearCacheLinks,
                        content = {
                            Icon(
                                painter = painterResource(id = R.drawable.database_icon),
                                contentDescription = stringResource(id = LocaleR.string.clear_cache_content_desc),
                                tint = LocalContentColor.current.onMediumEmphasis(0.8F)
                            )
                        }
                    )
                )
            )
        }
    }

}