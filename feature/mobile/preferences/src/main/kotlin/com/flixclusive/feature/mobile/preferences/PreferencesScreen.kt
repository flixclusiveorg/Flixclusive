package com.flixclusive.feature.mobile.preferences

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flixclusive.core.ui.common.navigation.navigator.PreferencesScreenNavigator
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.feature.mobile.preferences.component.PreferencesItem
import com.flixclusive.feature.mobile.preferences.component.ShareHeader
import com.ramcosta.composedestinations.annotation.Destination
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.locale.R as LocaleR

@Destination
@Composable
internal fun PreferencesScreen(
    navigator: PreferencesScreenNavigator
) {
    val items = remember {
        listOf(
            PreferencesNavigationItem(
                iconId = UiCommonR.drawable.round_library,
                labelId = LocaleR.string.watchlist,
                navigationAction = navigator::openWatchlistScreen
            ),
            PreferencesNavigationItem(
                iconId = R.drawable.time_circle,
                labelId = LocaleR.string.recently_watched,
                navigationAction = navigator::openRecentlyWatchedScreen
            ),
            PreferencesNavigationItem(
                iconId = UiCommonR.drawable.settings_filled,
                labelId = LocaleR.string.settings,
                navigationAction = navigator::openSettingsScreen
            ),
            PreferencesNavigationItem(
                iconId = UiCommonR.drawable.round_update_24,
                labelId = LocaleR.string.check_for_updates,
                navigationAction = navigator::checkForUpdates
            ),
            PreferencesNavigationItem(
                iconId = R.drawable.round_info_24,
                labelId = LocaleR.string.about,
                navigationAction = navigator::openAboutScreen
            )
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 15.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = stringResource(id = LocaleR.string.preferences),
                    style = MaterialTheme.typography.headlineMedium,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
            }
        }

        item {
            ShareHeader()
        }

        itemsIndexed(items) { i, item ->
            PreferencesItem(
                iconId = item.iconId,
                labelId = item.labelId,
                onClick = item.navigationAction
            )

            if (i < items.lastIndex)
                HorizontalDivider(
                    modifier = Modifier
                        .padding(horizontal = 25.dp),
                    thickness = 1.dp,
                    color = LocalContentColor.current.onMediumEmphasis(emphasis = 0.3F)
                )
        }
    }
}