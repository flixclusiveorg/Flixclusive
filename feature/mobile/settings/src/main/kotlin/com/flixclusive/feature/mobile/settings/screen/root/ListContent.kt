package com.flixclusive.feature.mobile.settings.screen.root

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.core.ui.common.user.UserAvatar
import com.flixclusive.core.ui.common.user.UserAvatarDefaults.DefaultAvatarSize
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.mobile.component.topbar.CommonTopBarDefaults.getTopBarHeadlinerTextStyle
import com.flixclusive.feature.mobile.settings.screen.BaseTweakNavigation
import com.flixclusive.feature.mobile.settings.screen.BaseTweakScreen
import com.flixclusive.feature.mobile.settings.util.getEmphasizedLabel
import com.flixclusive.feature.mobile.settings.util.getMediumEmphasizedLabel
import com.flixclusive.model.datastore.FlixclusivePrefs
import kotlinx.collections.immutable.ImmutableMap
import com.flixclusive.core.strings.R as LocaleR

internal val UserScreenHorizontalPadding = 16.dp
private val NavigationButtonHeight = 50.dp

@Composable
internal fun ListContent(
    appBuild: AppBuildWithPrereleaseFlag,
    items: ImmutableMap<Int?, List<BaseTweakScreen<out FlixclusivePrefs>>>,
    onScroll: (Float) -> Unit,
    currentUser: () -> User,
    navigator: SettingsScreenNavigator,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    var headerHeightPx by remember { mutableIntStateOf(0) }

    val onScrollCallback by rememberUpdatedState(onScroll)

    LaunchedEffect(listState, headerHeightPx) {
        snapshotFlow {
            Triple(listState.firstVisibleItemScrollOffset, listState.firstVisibleItemIndex, headerHeightPx)
        }.collect { (offset, index, headerHeight) ->
            val coercedOffset = offset.coerceIn(0, headerHeight).toFloat()

            val scrollOffset =
                when {
                    index == 0 && headerHeight > coercedOffset -> {
                        1f - (coercedOffset / headerHeight)
                    }

                    else -> 0F
                }

            onScrollCallback(scrollOffset)
        }
    }

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .then(modifier),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            ListContentHeader(
                currentUser = currentUser,
                onChangeUser = { navigator.openEditUserScreen(currentUser()) },
                modifier =
                    Modifier
                        .padding(bottom = 20.dp)
                        .padding(horizontal = UserScreenHorizontalPadding)
                        .onGloballyPositioned {
                            headerHeightPx = it.size.height
                        },
            )
        }

        items.forEach { (categoryLabel, buttons) ->
            item {
                HorizontalDivider(
                    modifier =
                        Modifier
                            .padding(
                                vertical = 15.dp,
                                horizontal = UserScreenHorizontalPadding,
                            ),
                    thickness = 1.dp,
                    color = LocalContentColor.current.onMediumEmphasis(emphasis = 0.2F),
                )
            }

            if (categoryLabel != null) {
                item {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp, top = 5.dp)
                                .padding(horizontal = UserScreenHorizontalPadding),
                    ) {
                        Text(
                            text = stringResource(id = categoryLabel),
                            style = getEmphasizedLabel(letterSpacing = 1.5.sp),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                        )
                    }
                }
            }

            items(buttons) { navigation ->
                if (navigation.isSubNavigation) return@items

                MenuItem(
                    icon = navigation.getIconPainter()!!,
                    label = navigation.getTitle(),
                    onClick = {
                        if (navigation is BaseTweakNavigation) {
                            navigation.onClick(navigator)
                            return@MenuItem
                        }

                        onItemClick(navigation.key.name)
                    },
                )
            }
        }

        item {
            HorizontalDivider(
                modifier =
                    Modifier
                        .padding(vertical = 15.dp),
                thickness = 1.dp,
                color = LocalContentColor.current.onMediumEmphasis(emphasis = 0.2F),
            )
        }

        item {
            ListContentFooter(
                versionName = appBuild.versionName,
                commitVersion = appBuild.commitVersion,
                isInDebugMode = appBuild.isDebug,
                isOnPreRelease = appBuild.isPrerelease,
            )
        }
    }
}

@Composable
private fun ListContentFooter(
    versionName: String,
    commitVersion: String,
    isInDebugMode: Boolean,
    isOnPreRelease: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val version =
        remember {
            versionName + (if (isOnPreRelease) "-[$commitVersion]" else "")
        }
    val mode =
        remember {
            when {
                isInDebugMode -> context.getString(LocaleR.string.debug)
                isOnPreRelease -> context.getString(LocaleR.string.pre_release)
                else -> context.getString(LocaleR.string.release)
            }
        }

    val defaultStyle =
        MaterialTheme.typography.headlineSmall
            .copy(
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                color = LocalContentColor.current.onMediumEmphasis(),
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
    currentUser: () -> User,
    onChangeUser: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            modifier
                .statusBarsPadding(),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 30.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = stringResource(id = LocaleR.string.settings),
                style = getTopBarHeadlinerTextStyle(),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }

        UserAvatar(
            user = currentUser(),
            modifier =
                Modifier
                    .clickable { onChangeUser() }
                    .size(DefaultAvatarSize),
        )

        Box(
            modifier =
                Modifier
                    .fillMaxWidth(0.4F),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = currentUser().name,
                style = getEmphasizedLabel(16.sp),
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                maxLines = 1,
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
