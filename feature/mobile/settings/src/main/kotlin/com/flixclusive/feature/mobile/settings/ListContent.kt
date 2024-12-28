package com.flixclusive.feature.mobile.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.navigation.navigator.SettingsScreenNavigator
import com.flixclusive.core.ui.common.user.UserAvatar
import com.flixclusive.core.ui.common.user.UserAvatarDefaults.DefaultAvatarSize
import com.flixclusive.core.ui.common.util.DummyDataForPreview.getDummyUser
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.feature.mobile.settings.screen.BaseTweakNavigation
import com.flixclusive.feature.mobile.settings.screen.BaseTweakScreen
import com.flixclusive.feature.mobile.settings.screen.advanced.AdvancedTweakScreen
import com.flixclusive.feature.mobile.settings.screen.appearance.AppearanceTweakScreen
import com.flixclusive.feature.mobile.settings.screen.data.DataTweakScreen
import com.flixclusive.feature.mobile.settings.screen.general.GeneralTweakScreen
import com.flixclusive.feature.mobile.settings.screen.github.FeatureRequestTweakNavigation
import com.flixclusive.feature.mobile.settings.screen.github.IssueBugTweakNavigation
import com.flixclusive.feature.mobile.settings.screen.github.RepositoryTweakNavigation
import com.flixclusive.feature.mobile.settings.screen.player.PlayerTweakScreen
import com.flixclusive.feature.mobile.settings.screen.providers.ProvidersTweakNavigation
import com.flixclusive.feature.mobile.settings.util.UiUtil.getEmphasizedLabel
import com.flixclusive.feature.mobile.settings.util.UiUtil.getMediumEmphasizedLabel
import com.flixclusive.model.database.User
import com.flixclusive.core.locale.R as LocaleR

internal val UserScreenHorizontalPadding = 16.dp
private val NavigationButtonHeight = 50.dp

@Composable
internal fun ListContent(
    modifier: Modifier = Modifier,
    currentUser: () -> User,
    searchHistoryCount: Int = 0,
    onClearSearchHistory: () -> Unit,
    navigator: SettingsScreenNavigator,
    onItemClick: (BaseTweakScreen) -> Unit,
) {
    val items = remember {
        mapOf(
            LocaleR.string.application to listOf(
                GeneralTweakScreen,
                ProvidersTweakNavigation,
                AppearanceTweakScreen,
                PlayerTweakScreen,
                DataTweakScreen(
                    searchHistoryCount = searchHistoryCount,
                    onClearSearchHistory = onClearSearchHistory
                ),
                AdvancedTweakScreen
            ),
            LocaleR.string.github to listOf(
                IssueBugTweakNavigation,
                FeatureRequestTweakNavigation,
                RepositoryTweakNavigation
            )
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier),
        contentPadding = PaddingValues(horizontal = UserScreenHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 10.dp, bottom = 30.dp),
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
            ListContentHeader(
                currentUser = currentUser,
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
                MenuItem(
                    icon = navigation.getIconPainter()!!,
                    label = navigation.getTitle(),
                    onClick = {
                        if (navigation is BaseTweakNavigation) {
                            navigation.onClick(navigator)
                            return@MenuItem
                        }

                        onItemClick(navigation)
                    }
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
            ListContentFooter(
                versionName = "1.0.0",
                commitVersion = "a1e62eq",
                isInDebugMode = false,
                isOnPreRelease = false
            )
        }
    }
}

@Composable
private fun ListContentFooter(
    modifier: Modifier = Modifier,
    versionName: String,
    commitVersion: String,
    isInDebugMode: Boolean,
    isOnPreRelease: Boolean,
) {
    val context = LocalContext.current
    val version = remember {
        versionName + (if (isOnPreRelease) "-[$commitVersion]" else "")
    }
    val mode = remember {
        when {
            isInDebugMode -> context.getString(LocaleR.string.debug)
            isOnPreRelease -> context.getString(LocaleR.string.pre_release)
            else -> context.getString(LocaleR.string.release)
        }
    }

    val defaultStyle = MaterialTheme.typography.headlineSmall
        .copy(
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            color = LocalContentColor.current.onMediumEmphasis()
        )

    Box(
        modifier = modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(defaultStyle.toSpanStyle()) {
                    append(version)
                    append(" — ")
                    append(mode)
                }
            },
        )
    }
}

@Composable
private fun ListContentHeader(
    modifier: Modifier = Modifier,
    currentUser: () -> User,
    onChangeUser: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .statusBarsPadding()
    ) {
        UserAvatar(
            user = currentUser(),
            modifier = Modifier
                .size(DefaultAvatarSize)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth(0.4F),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = currentUser().name,
                style = getEmphasizedLabel(16.sp),
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MenuItem(
    icon: Painter,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(NavigationButtonHeight)
            .clip(MaterialTheme.shapes.extraSmall)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(35.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = icon,
                contentDescription = label
            )
        }

        Text(
            text = label,
            style = getMediumEmphasizedLabel(size = 16.sp),
            modifier = Modifier
                .padding(start = 13.dp)
        )
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
            ListContent(
                currentUser = { getDummyUser() },
                searchHistoryCount = 1,
                onClearSearchHistory = {},
                navigator = getNavigatorPreview(),
                onItemClick = {}
            )
        }
    }
}