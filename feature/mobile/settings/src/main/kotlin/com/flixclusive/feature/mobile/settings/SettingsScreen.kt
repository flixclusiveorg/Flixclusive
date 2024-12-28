package com.flixclusive.feature.mobile.settings

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
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.palette.graphics.Palette
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.navigation.navigator.SettingsScreenNavigator
import com.flixclusive.core.ui.common.user.getAvatarResource
import com.flixclusive.core.ui.common.util.DummyDataForPreview.getDummyUser
import com.flixclusive.feature.mobile.settings.screen.BaseTweakScreen
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.LocalAppSettings
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.LocalAppSettingsProvider
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.LocalScaffoldNavigator
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.LocalSettingsViewModel
import com.flixclusive.model.database.User
import com.ramcosta.composedestinations.annotation.Destination

@SuppressLint("UnusedContentLambdaTargetStateParameter")
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Destination
@Composable
internal fun SettingsScreen(
    navigator: SettingsScreenNavigator
) {
    val viewModel = hiltViewModel<SettingsViewModel>()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val appSettingsProvider by viewModel.appSettingsProvider.collectAsStateWithLifecycle()
    val currentUser by viewModel.userSessionManager.currentUser.collectAsStateWithLifecycle()
    val searchHistoryCount by viewModel.searchHistoryCount.collectAsStateWithLifecycle()

    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<BaseTweakScreen>()
    val isListAndDetailVisible =
        scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Expanded
        && scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded

    val backgroundBrush = getAdaptiveBackground(currentUser)

    BackHandler(scaffoldNavigator.canNavigateBack()) {
        scaffoldNavigator.navigateBack()
    }

    CompositionLocalProvider(
        LocalAppSettings provides appSettings,
        LocalAppSettingsProvider provides appSettingsProvider,
        LocalScaffoldNavigator provides scaffoldNavigator,
        LocalSettingsViewModel provides viewModel,
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
                    val isDetailVisible =
                        scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Expanded

                    AnimatedPane {
                        ListContent(
                            currentUser = { currentUser ?: getDummyUser() /*TODO: Remove elvis fallback */ },
                            searchHistoryCount = searchHistoryCount,
                            onClearSearchHistory = viewModel::clearSearchHistory,
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
                                    scaffoldNavigator.navigateBack()
                                }
                            },
                            content = {
                                scaffoldNavigator.currentDestination?.content?.Content()
                            }
                        )
                    }
                }
            )
        }
    }

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