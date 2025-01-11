package com.flixclusive.feature.mobile.settings.screen.root

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.util.fastFirstOrNull
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.palette.graphics.Palette
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.navigation.GoBackAction
import com.flixclusive.core.ui.common.user.getAvatarResource
import com.flixclusive.feature.mobile.settings.screen.BaseTweakNavigation
import com.flixclusive.feature.mobile.settings.screen.appearance.AppearanceTweakScreen
import com.flixclusive.feature.mobile.settings.screen.data.DataTweakScreen
import com.flixclusive.feature.mobile.settings.screen.github.FeatureRequestTweakNavigation
import com.flixclusive.feature.mobile.settings.screen.github.IssueBugTweakNavigation
import com.flixclusive.feature.mobile.settings.screen.github.RepositoryTweakNavigation
import com.flixclusive.feature.mobile.settings.screen.player.PlayerTweakScreen
import com.flixclusive.feature.mobile.settings.screen.providers.ProvidersTweakScreen
import com.flixclusive.feature.mobile.settings.screen.subtitles.SubtitlesTweakScreen
import com.flixclusive.feature.mobile.settings.screen.system.SystemTweakScreen
import com.flixclusive.feature.mobile.settings.util.LocalScaffoldNavigator
import com.flixclusive.feature.mobile.settings.util.LocalSettingsNavigator
import com.flixclusive.model.database.User
import com.flixclusive.model.datastore.user.UserPreferences
import com.ramcosta.composedestinations.annotation.Destination
import com.flixclusive.core.locale.R as LocaleR

@Suppress("ktlint:compose:vm-forwarding-check")
@SuppressLint("UnusedContentLambdaTargetStateParameter")
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Destination
@Composable
internal fun SettingsScreen(
    navigator: SettingsScreenNavigator,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val currentUser by viewModel.userSessionManager.currentUser.collectAsStateWithLifecycle()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val initialNavigation =
        if (isLandscape) {
            listOf(
                ThreePaneScaffoldDestinationItem(
                    ListDetailPaneScaffoldRole.Detail,
                    UserPreferences.UI_PREFS_KEY.name,
                ),
            )
        } else {
            listOf(ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.List))
        }

    val scaffoldNavigator =
        rememberListDetailPaneScaffoldNavigator<String>(initialDestinationHistory = initialNavigation)
    val isListAndDetailVisible =
        scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded &&
            scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Expanded

    val backgroundAlpha = rememberSaveable { mutableFloatStateOf(1F) }
    val backgroundBrush = getAdaptiveBackground(currentUser)

    val items =
        remember {
            mapOf(
                LocaleR.string.application to
                    listOf(
                        AppearanceTweakScreen(viewModel),
                        PlayerTweakScreen(viewModel),
                        DataTweakScreen(viewModel),
                        ProvidersTweakScreen(viewModel),
                        SubtitlesTweakScreen(viewModel),
                        SystemTweakScreen(viewModel),
                    ),
                LocaleR.string.github to
                    listOf(
                        IssueBugTweakNavigation,
                        FeatureRequestTweakNavigation,
                        RepositoryTweakNavigation,
                    ),
            )
        }
    val navigationItems = remember { items.values.flatten() }

    BackHandler(scaffoldNavigator.canNavigateBack()) {
        scaffoldNavigator.navigateBack()
    }

    if (currentUser != null) {
        CompositionLocalProvider(
            LocalScaffoldNavigator provides scaffoldNavigator,
            LocalSettingsNavigator provides navigator,
        ) {
            ListDetailPaneScaffold(
                modifier =
                    Modifier.drawBehind {
                        if (isListAndDetailVisible) {
                            drawRect(backgroundBrush, alpha = backgroundAlpha.floatValue)
                        }
                    },
                directive = scaffoldNavigator.scaffoldDirective,
                value = scaffoldNavigator.scaffoldValue,
                listPane = {
                    AnimatedPane {
                        ListContent(
                            items = items,
                            currentUser = { currentUser!! },
                            navigator = navigator,
                            onScroll = { backgroundAlpha.floatValue = it },
                            onItemClick = { item ->
                                scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail, item)
                            },
                            modifier =
                                Modifier.drawBehind {
                                    if (!isListAndDetailVisible) {
                                        drawRect(backgroundBrush)
                                    }
                                },
                        )
                    }
                },
                detailPane = {
                    val screen by remember {
                        derivedStateOf {
                            navigationItems.fastFirstOrNull {
                                if (it is BaseTweakNavigation) {
                                    return@fastFirstOrNull false
                                }

                                it.key.name == scaffoldNavigator.currentDestination?.content
                            }
                        }
                    }

                    if (screen != null) {
                        val isListVisible =
                            scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.List] ==
                                PaneAdaptedValue.Expanded

                        AnimatedPane {
                            AnimatedContent(
                                targetState = screen!!,
                                label = "DetailsContent",
                                transitionSpec = {
                                    if (initialState.isSubNavigation == true) {
                                        fadeIn() + slideInHorizontally { -it / 4 } togetherWith
                                            slideOutHorizontally { it / 4 } + fadeOut()
                                    } else {
                                        fadeIn() + slideInHorizontally { it / 4 } togetherWith
                                            slideOutHorizontally { -it / 4 } + fadeOut()
                                    }
                                },
                            ) {
                                DetailsScaffold(
                                    isListAndDetailVisible = isListAndDetailVisible,
                                    isDetailsVisible = !isListVisible,
                                    content = { it.Content() },
                                    navigateBack = {
                                        if (scaffoldNavigator.canNavigateBack()) {
                                            scaffoldNavigator.navigateBack(
                                                backNavigationBehavior = BackNavigationBehavior.PopLatest,
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun getAdaptiveBackground(currentUser: User?): Brush {
    val context = LocalContext.current

    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary.copy(0.3F)

    return remember(currentUser?.image) {
        val colors =
            if (currentUser != null) {
                val avatarId = context.getAvatarResource(currentUser.image)
                val drawable = ContextCompat.getDrawable(context, avatarId)!!

                val palette =
                    Palette
                        .from(drawable.toBitmap())
                        .generate()

                val backgroundColor = Color(palette.dominantSwatch?.rgb ?: surfaceColor.toArgb())
                val tubeLightColor = Color(palette.lightVibrantSwatch?.rgb ?: surfaceColor.toArgb())

                listOf(
                    tubeLightColor.copy(0.5F),
                    backgroundColor.copy(0.2F),
                    surfaceColor,
                )
            } else {
                listOf(primaryColor, surfaceColor)
            }

        Brush.verticalGradient(colors)
    }
}

interface SettingsScreenNavigator : GoBackAction {
    fun openProvidersScreen()

    fun openLink(url: String)
}

internal fun getNavigatorPreview() =
    object : SettingsScreenNavigator {
        override fun openProvidersScreen() = Unit

        override fun openLink(url: String) = Unit

        override fun goBack() = Unit
    }

@Preview(device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
private fun TabletPreview() {
    FlixclusiveTheme {
        Surface {
            SettingsScreen(
                navigator = getNavigatorPreview(),
            )
        }
    }
}

@Preview(device = "spec:width=411dp,height=891dp")
@Composable
private fun PhonePreview() {
    FlixclusiveTheme {
        Surface {
            SettingsScreen(
                navigator = getNavigatorPreview(),
            )
        }
    }
}
