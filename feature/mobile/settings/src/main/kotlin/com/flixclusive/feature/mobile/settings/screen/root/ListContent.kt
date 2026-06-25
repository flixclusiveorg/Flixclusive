package com.flixclusive.feature.mobile.settings.screen.root

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.common.config.AppVersion
import com.flixclusive.core.common.config.BuildType
import com.flixclusive.core.common.config.CustomBuildConfig
import com.flixclusive.core.common.config.PlatformType
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.core.navigation.settings.SubSettingsNavItem
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.UserAvatar
import com.flixclusive.core.presentation.mobile.components.material3.topbar.CommonTopBarDefaults.getTopBarHeadlinerTextStyle
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.feature.mobile.settings.R
import com.flixclusive.feature.mobile.settings.util.getEmphasizedLabel
import com.flixclusive.feature.mobile.settings.util.getMediumEmphasizedLabel
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

internal val UserScreenHorizontalPadding = 16.dp
private val NavigationButtonHeight = 50.dp

@Composable
internal fun ListContent(
    buildConfig: CustomBuildConfig,
    currentUser: () -> User,
    navigator: NavigatorSettingsScreen,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .then(modifier),
    ) {
        item {
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 10.dp, bottom = 30.dp)
                    .padding(horizontal = UserScreenHorizontalPadding),
            ) {
                Text(
                    text = stringResource(id = LocaleR.string.settings),
                    style = getTopBarHeadlinerTextStyle(),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            }
        }

        item {
            ListContentHeader(
                currentUser = currentUser,
                onChangeUser = { navigator.navigateToUserProfilesScreen() },
                onEditUser = { navigator.navigateToEditUserScreen(currentUser().id) },
                modifier = Modifier
                    .padding(bottom = 20.dp)
                    .padding(horizontal = UserScreenHorizontalPadding)
            )
        }

        navigationItems(
            title = LocaleR.string.application,
        ) {
            items(SubSettingsNavItem.entries) { navigation ->
                val iconId = when (navigation) {
                    SubSettingsNavItem.APPEARANCE -> UiCommonR.drawable.appearance_settings
                    SubSettingsNavItem.PLAYER -> UiCommonR.drawable.play_outline_circle
                    SubSettingsNavItem.DATA -> UiCommonR.drawable.database_icon_thin
                    SubSettingsNavItem.PROVIDERS -> UiCommonR.drawable.provider_logo
                    SubSettingsNavItem.SYSTEM -> UiCommonR.drawable.wrench
                }

                val stringId = when (navigation) {
                    SubSettingsNavItem.APPEARANCE -> LocaleR.string.appearance
                    SubSettingsNavItem.PLAYER -> LocaleR.string.player
                    SubSettingsNavItem.DATA -> LocaleR.string.data
                    SubSettingsNavItem.PROVIDERS -> LocaleR.string.providers
                    SubSettingsNavItem.SYSTEM -> LocaleR.string.system
                }

                MenuItem(
                    icon = painterResource(id = iconId),
                    label = stringResource(id = stringId),
                    onClick = { navigator.navigateToSubSettingsScreen(navigation) },
                )
            }
        }

        navigationItems(
            title = LocaleR.string.application,
        ) {
            items(GithubNavigation.entries) { navigation ->
                val uriHandler = LocalUriHandler.current
                val iconId = when (navigation) {
                    GithubNavigation.FEATURE_REQUEST -> UiCommonR.drawable.feature_request
                    GithubNavigation.BUG_REPORT -> UiCommonR.drawable.bug_thin
                    GithubNavigation.REPOSITORY -> UiCommonR.drawable.github_outline
                }

                val stringId = when (navigation) {
                    GithubNavigation.FEATURE_REQUEST -> R.string.feature_request
                    GithubNavigation.BUG_REPORT -> R.string.report_a_bug
                    GithubNavigation.REPOSITORY -> LocaleR.string.repository
                }

                MenuItem(
                    icon = painterResource(id = iconId),
                    label = stringResource(id = stringId),
                    onClick = { uriHandler.openUri(navigation.uri) },
                )
            }
        }

        item {
            HorizontalDivider(
                modifier =
                    Modifier
                        .padding(vertical = 15.dp),
                thickness = 1.dp,
                color = LocalContentColor.current.copy(alpha = 0.2F),
            )
        }

        item {
            ListContentFooter(
                version = buildConfig.version,
                buildType = buildConfig.buildType,
            )
        }
    }
}

private fun LazyListScope.navigationItems(
    @StringRes title: Int,
    content: LazyListScope.() -> Unit,
) {
    item {
        HorizontalDivider(
            thickness = 1.dp,
            color = LocalContentColor.current.copy(alpha = 0.2F),
            modifier = Modifier
                .padding(
                    vertical = 15.dp,
                    horizontal = UserScreenHorizontalPadding,
                ),
        )
    }

    item {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp, top = 5.dp)
                .padding(horizontal = UserScreenHorizontalPadding),
        ) {
            Text(
                text = stringResource(title),
                style = getEmphasizedLabel(letterSpacing = 1.5.sp),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
    }

    content()
}

@Composable
private fun ListContentFooter(
    version: AppVersion,
    buildType: BuildType,
    modifier: Modifier = Modifier,
) {
    val resources = LocalResources.current
    val mode = remember {
        when {
            buildType.isDebug -> resources.getString(LocaleR.string.debug)
            buildType.isPreview -> resources.getString(LocaleR.string.pre_release)
            else -> resources.getString(LocaleR.string.release)
        }
    }

    val defaultStyle =
        MaterialTheme.typography.headlineSmall
            .copy(
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                color = LocalContentColor.current.copy(0.6f),
            )

    Box(
        modifier =
            modifier
                .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text =
                buildAnnotatedString {
                    withStyle(defaultStyle.toSpanStyle()) {
                        append(version.toString())
                        append(" — ")
                        append(mode)
                    }
                },
        )
    }
}

@Composable
private fun ListContentHeader(
    currentUser: () -> User,
    onChangeUser: () -> Unit,
    onEditUser: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable { onChangeUser() }
            .fillMaxWidth(),
    ) {
        Box {
            UserAvatar(
                avatar = currentUser().image,
                modifier = Modifier
                    .clickable { onEditUser() }
                    .size(getAdaptiveDp(dp = 65.dp, increaseBy = 20.dp)),
            )

            Icon(
                painter = painterResource(id = UiCommonR.drawable.edit),
                contentDescription = stringResource(id = LocaleR.string.edit_profile),
                tint = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .padding(top = 10.dp, start = 10.dp)
                    .align(Alignment.BottomEnd)
                    .size(22.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        MaterialTheme.colorScheme.onSurface,
                        MaterialTheme.shapes.small,
                    ).padding(3.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = currentUser().name,
                style = getEmphasizedLabel(16.sp),
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
            )

            Text(
                text = currentUser().id,
                style = MaterialTheme.typography.labelSmall,
                color = LocalContentColor.current.copy(0.6f),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }

        AdaptiveIcon(
            painter = painterResource(id = UiCommonR.drawable.arrow_right_thin),
            contentDescription = stringResource(id = LocaleR.string.switch_profile),
        )
    }
}

@Composable
private fun MenuItem(
    icon: Painter,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .clip(MaterialTheme.shapes.extraSmall)
                .clickable(onClick = onClick)
                .fillMaxWidth()
                .height(NavigationButtonHeight)
                .padding(horizontal = UserScreenHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .width(35.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = icon,
                contentDescription = label,
            )
        }

        Text(
            text = label,
            style = getMediumEmphasizedLabel(size = 16.sp),
            modifier =
                Modifier
                    .padding(start = 13.dp),
        )
    }
}

@Preview
@Composable
private fun ListContentPreview() {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            ListContent(
                buildConfig = CustomBuildConfig(
                    version = AppVersion.from(BuildType.PREVIEW, "10"),
                    buildType = BuildType.DEBUG,
                    applicationName = "Flixclusive",
                    applicationId = "com.flixclusive",
                    platformType = PlatformType.MOBILE
                ),
                currentUser = {
                    User(
                        id = "1",
                        name = "John Doe",
                        image = 1,
                    )
                },
                navigator = object : NavigatorSettingsScreen {
                    override fun navigateToProviderManagerScreen() = Unit

                    override fun navigateToRepositoryManagerScreen() = Unit

                    override fun navigateToUrl(url: String) = Unit

                    override fun navigateBack() = Unit

                    override fun navigateToUserProfilesScreen(shouldPopBackStack: Boolean) = Unit

                    override fun navigateToEditUserScreen(userId: String) = Unit

                    override fun showMediaPreviewBottomSheet(media: MediaMetadata) = Unit

                    override fun navigateToSubSettingsScreen(route: SubSettingsNavItem) = Unit
                },
            )
        }
    }
}
