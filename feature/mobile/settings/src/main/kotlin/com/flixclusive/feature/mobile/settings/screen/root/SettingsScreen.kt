package com.flixclusive.feature.mobile.settings.screen.root

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.flixclusive.feature.mobile.settings.screen.BaseTweakScreen
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
import com.flixclusive.model.datastore.FlixclusivePrefs
import com.ramcosta.composedestinations.annotation.Destination
import com.flixclusive.core.locale.R as LocaleR

@SuppressLint("UnusedContentLambdaTargetStateParameter")
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Destination
@Composable
internal fun SettingsScreen(
    navigator: SettingsScreenNavigator
) {
    val viewModel = hiltViewModel<SettingsViewModel>()
    val currentUser by viewModel.userSessionManager.currentUser.collectAsStateWithLifecycle()

    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<String>()
    val isListAndDetailVisible =
        scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Expanded
        && scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded

    val backgroundBrush = getAdaptiveBackground(currentUser)

    val items = remember {
        mapOf(
            LocaleR.string.application to listOf(
                AppearanceTweakScreen(viewModel),
                PlayerTweakScreen(viewModel),
                DataTweakScreen(viewModel),
                ProvidersTweakScreen(viewModel),
                SubtitlesTweakScreen(viewModel),
                SystemTweakScreen(viewModel),
            ),
            LocaleR.string.github to listOf(
                IssueBugTweakNavigation,
                FeatureRequestTweakNavigation,
                RepositoryTweakNavigation
            )
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
            AnimatedContent(targetState = isListAndDetailVisible, label = "settings screen") {
                ListDetailPaneScaffold(
                    modifier = Modifier.drawBehind {
                        if (isListAndDetailVisible) {
                            drawRect(backgroundBrush)
                        }
                    },
                    directive = scaffoldNavigator.scaffoldDirective,
                    value = scaffoldNavigator.scaffoldValue,
                    listPane = {
                        AnimatedPane {
                            ListContent(
                                viewModel = viewModel,
                                items = items,
                                currentUser = { currentUser!! },
                                navigator = navigator,
                                onItemClick = { item ->
                                    scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail, item)
                                },
                                modifier = Modifier.drawBehind {
                                    if (!isListAndDetailVisible) {
                                        drawRect(backgroundBrush)
                                    }
                                },
                            )
                        }
                    },
                    detailPane = {
                        val isListVisible =
                            scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded

                        AnimatedPane {
                            DetailsScaffold(
                                isListAndDetailVisible = isListAndDetailVisible,
                                isDetailsVisible = !isListVisible,
                                navigateBack = {
                                    if (scaffoldNavigator.canNavigateBack()) {
                                        scaffoldNavigator.navigateBack(
                                            backNavigationBehavior = BackNavigationBehavior.PopLatest
                                        )
                                    }
                                },
                                content = {
                                    RenderTweakScreen(
                                        key = scaffoldNavigator.currentDestination?.content,
                                        items = navigationItems
                                    )
                                }
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun RenderTweakScreen(
    key: String?, items: List<BaseTweakScreen<out FlixclusivePrefs>>
) {
    items.fastFirstOrNull {
        if (it is BaseTweakNavigation)
            return@fastFirstOrNull false

        it.key.name == key
    }?.Content()
}

@Composable
private fun getAdaptiveBackground(
    currentUser: User?,
): Brush {
    val context = LocalContext.current

    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary

    return remember(currentUser?.image) {
        val color = if (currentUser != null) {
            val avatarId = context.getAvatarResource(currentUser.image)
            val drawable = ContextCompat.getDrawable(context, avatarId)!!

            val palette = Palette
                .from(drawable.toBitmap())
                .generate()

            val swatch = palette.let {
                it.vibrantSwatch
                    ?: it.lightVibrantSwatch
                    ?: it.lightMutedSwatch
            }

            swatch?.rgb?.let { Color(it) }
                ?: primaryColor
        } else surfaceColor

        Brush.verticalGradient(
            0F to color.copy(0.3F),
            0.4F to surfaceColor
        )
    }
}

interface SettingsScreenNavigator : GoBackAction {
    fun openProvidersScreen()
    fun openLink(url: String)
}

internal fun getNavigatorPreview() = object : SettingsScreenNavigator {
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
                navigator = getNavigatorPreview()
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
                navigator = getNavigatorPreview()
            )
        }
    }
}