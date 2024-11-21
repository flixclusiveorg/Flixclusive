package com.flixclusive.feature.mobile.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.navigation.navigator.PreferencesScreenNavigator
import com.flixclusive.core.ui.common.user.UserAvatar
import com.flixclusive.core.ui.common.user.getAvatarResource
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.feature.mobile.settings.util.UiUtil.getEmphasizedLabel
import com.flixclusive.feature.mobile.settings.util.UiUtil.getMediumEmphasizedLabel
import com.flixclusive.model.database.User
import com.ramcosta.composedestinations.annotation.Destination
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

internal val UserScreenHorizontalPadding = 16.dp
private val NavigationButtonHeight = 50.dp

@Destination
@Composable
internal fun SettingsScreen(
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

    val context = LocalContext.current
    val currentUser = remember { User(image = 1) }

    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary
    val avatarId = remember(currentUser.image) {
        context.getAvatarResource(currentUser.image)
    }

    val backgroundColor = remember(avatarId) {
        val drawable = ContextCompat.getDrawable(context, avatarId)!!
        val palette = Palette
            .from(drawable.toBitmap())
            .generate()

        val swatch = palette.let {
            it.vibrantSwatch
            ?: it.lightVibrantSwatch
            ?: it.lightMutedSwatch
        }

        val color = swatch?.rgb?.let { Color(it) }
            ?: primaryColor

        Brush.verticalGradient(
            0F to color.copy(0.3F),
            0.4F to surfaceColor
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
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
            SettingsHeader(
                currentUser = { currentUser },
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
            SettingsFooter(
                versionName = "1.0.0",
                commitVersion = "a1e62eq",
                isInDebugMode = false,
                isOnPreRelease = false
            )
        }
    }
}

@Composable
private fun SettingsFooter(
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
private fun SettingsHeader(
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
        UserAvatar(user = currentUser())

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
private fun SettingsNavigationButton(
    @DrawableRes iconId: Int,
    @StringRes labelId: Int,
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
                painter = painterResource(id = iconId),
                contentDescription = stringResource(id = labelId)
            )
        }

        Text(
            text = stringResource(id = labelId),
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
            SettingsScreen(
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