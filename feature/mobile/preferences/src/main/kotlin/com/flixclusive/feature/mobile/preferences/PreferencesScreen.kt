package com.flixclusive.feature.mobile.preferences

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.navigation.navigator.PreferencesScreenNavigator
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.feature.mobile.preferences.component.AppVersionFooter
import com.flixclusive.feature.mobile.preferences.component.SettingsNavigationButton
import com.flixclusive.feature.mobile.preferences.user.UserProfilePicture
import com.flixclusive.feature.mobile.preferences.util.UiUtil.getEmphasizedLabel
import com.flixclusive.model.database.User
import com.ramcosta.composedestinations.annotation.Destination
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

internal val UserScreenHorizontalPadding = 16.dp

@Destination
@Composable
internal fun PreferencesScreen(
    navigator: PreferencesScreenNavigator
) {
    val items = remember {
        mapOf(
            LocaleR.string.application to listOf(
                PreferencesNavigationItem(
                    iconId = UiCommonR.drawable.general_settings,
                    labelId = LocaleR.string.general,
                    navigationAction = navigator::openWatchlistScreen
                ),
                PreferencesNavigationItem(
                    iconId = UiCommonR.drawable.provider_logo,
                    labelId = LocaleR.string.providers,
                    navigationAction = navigator::openWatchlistScreen
                ),
                PreferencesNavigationItem(
                    iconId = UiCommonR.drawable.appearance_settings,
                    labelId = LocaleR.string.appearance,
                    navigationAction = navigator::openRecentlyWatchedScreen
                ),
                PreferencesNavigationItem(
                    iconId = UiCommonR.drawable.play_outline_circle,
                    labelId = LocaleR.string.player,
                    navigationAction = navigator::openSettingsScreen
                ),
                PreferencesNavigationItem(
                    iconId = UiCommonR.drawable.database_icon_thin,
                    labelId = LocaleR.string.data_and_backup,
                    navigationAction = navigator::checkForUpdates
                ),
                PreferencesNavigationItem(
                    iconId = UiCommonR.drawable.code,
                    labelId = LocaleR.string.advanced,
                    navigationAction = navigator::openAboutScreen
                )
            ),
            LocaleR.string.github to listOf(
                PreferencesNavigationItem(
                    iconId = UiCommonR.drawable.test_thin,
                    labelId = LocaleR.string.issue_a_bug,
                    navigationAction = navigator::openWatchlistScreen
                ),
                PreferencesNavigationItem(
                    iconId = UiCommonR.drawable.feature_request,
                    labelId = LocaleR.string.feature_request,
                    navigationAction = navigator::openWatchlistScreen
                ),
                PreferencesNavigationItem(
                    iconId = UiCommonR.drawable.github_outline,
                    labelId = LocaleR.string.repository,
                    navigationAction = navigator::openRecentlyWatchedScreen
                ),
            )
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = UserScreenHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(bottom = 30.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = stringResource(id = LocaleR.string.settings),
                    style = MaterialTheme.typography.headlineMedium,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
            }
        }

        item {
            UserProfilePicture(
                currentUser = User(),
                onChangeUser = { /*TODO*/ },
                modifier = Modifier
                    .padding(bottom = 20.dp)
            )
        }

        items.forEach { (categoryLabel, buttons) ->
            item {
                HorizontalDivider(
                    modifier = Modifier
                        .padding(vertical = 15.dp),
                    thickness = 1.dp,
                    color = LocalContentColor.current.onMediumEmphasis(emphasis = 0.2F)
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp, top = 5.dp)
                ) {
                    Text(
                        text = stringResource(id = categoryLabel),
                        style = getEmphasizedLabel(letterSpacing = 1.5.sp),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                }
            }

            items(buttons) { navigation ->
                SettingsNavigationButton(
                    iconId = navigation.iconId,
                    labelId = navigation.labelId,
                    onClick = navigation.navigationAction
                )
            }
        }

        item {
            HorizontalDivider(
                modifier = Modifier
                    .padding(vertical = 15.dp),
                thickness = 1.dp,
                color = LocalContentColor.current.onMediumEmphasis(emphasis = 0.2F)
            )
        }

        item {
            AppVersionFooter(
                versionName = "1.0.0",
                commitVersion = "a1e62eq",
                isInDebugMode = false,
                isOnPreRelease = false
            )
        }
    }
}

@Preview
@Composable
private fun PreferencesScreenPreview() {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
        ) {
            PreferencesScreen(
                navigator = object : PreferencesScreenNavigator {
                    override fun openWatchlistScreen() {}
                    override fun openRecentlyWatchedScreen() {}
                    override fun openSettingsScreen() {}
                    override fun checkForUpdates() {}
                    override fun openAboutScreen() {}
                }
            )
        }
    }
}