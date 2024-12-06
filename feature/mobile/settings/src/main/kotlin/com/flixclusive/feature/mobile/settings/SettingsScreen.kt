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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateMap
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
import com.flixclusive.core.ui.common.user.getAvatarResource
import com.flixclusive.feature.mobile.settings.screen.BaseTweakScreen
import com.flixclusive.feature.mobile.settings.screen.general.GeneralSettingsScreen
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.KEY_AUDIO_LANGUAGE_DIALOG
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.KEY_DECODER_PRIORITY_DIALOG
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.KEY_DOH_DIALOG
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.KEY_PLAYER_BUFFER_LENGTH_DIALOG
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.KEY_PLAYER_BUFFER_SIZE_DIALOG
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.KEY_PLAYER_DISK_CACHE_DIALOG
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.KEY_PLAYER_QUALITY_DIALOG
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.KEY_PLAYER_RESIZE_MODE_DIALOG
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.KEY_PLAYER_SEEK_INCREMENT_MS_DIALOG
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.KEY_PREFERRED_SERVER_DIALOG
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.KEY_SEARCH_HISTORY_NOTICE_DIALOG
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.KEY_SUBTITLE_BACKGROUND_COLOR_DIALOG
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.KEY_SUBTITLE_COLOR_DIALOG
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.KEY_SUBTITLE_EDGE_TYPE_DIALOG
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.KEY_SUBTITLE_FONT_STYLE_DIALOG
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.KEY_SUBTITLE_LANGUAGE_DIALOG
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.KEY_SUBTITLE_SIZE_DIALOG
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.LocalDialogKeyMap
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.LocalScaffoldNavigator
import com.flixclusive.model.database.User

@SuppressLint("UnusedContentLambdaTargetStateParameter")
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun SettingsScreen() {
    val viewModel = hiltViewModel<SettingsViewModel>()
    val currentUser by viewModel.userSessionManager.currentUser.collectAsStateWithLifecycle()

    val navigator = rememberListDetailPaneScaffoldNavigator<BaseTweakScreen>()
    val isListAndDetailVisible =
        navigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Expanded
        && navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded

    val backgroundBrush = getAdaptiveBackground(currentUser)

    val dialogKeyMap = rememberSaveable { getDialogKeys() }

    BackHandler(navigator.canNavigateBack()) {
        navigator.navigateBack()
    }

    CompositionLocalProvider(
        LocalScaffoldNavigator provides navigator
    ) {
        AnimatedContent(targetState = isListAndDetailVisible, label = "settings screen") {
            ListDetailPaneScaffold(
                modifier = Modifier.drawBehind {
                    if (isListAndDetailVisible) {
                        drawRect(backgroundBrush)
                    }
                },
                directive = navigator.scaffoldDirective,
                value = navigator.scaffoldValue,
                listPane = {
                    val isDetailVisible =
                        navigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Expanded

                    AnimatedPane {
                        ListContent(
                            currentUser = { currentUser!! },
                            onItemClick = { item ->
                                navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, item)
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
                        navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded

                    AnimatedPane {
                        CompositionLocalProvider(
                            LocalDialogKeyMap provides dialogKeyMap
                        ) {
                            DetailsScaffold(
                                isListAndDetailVisible = isListAndDetailVisible,
                                isDetailsVisible = !isListVisible,
                                navigateBack = {
                                    if (navigator.canNavigateBack()) {
                                        navigator.navigateBack()
                                    }
                                },
                                content = {
                                    navigator.currentDestination?.content?.let { item ->
                                        NavigatedScreen(
                                            listItem = item,
                                            viewModel = viewModel
                                        )
                                    }
                                }
                            )
                        }
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

@Composable
private fun NavigatedScreen(
    listItem: ListItem,
    viewModel: SettingsViewModel
) {
    when (listItem) {
        ListItem.GENERAL_SETTINGS -> {
            val searchHistoryCount by viewModel.searchHistoryCount.collectAsStateWithLifecycle()

            GeneralSettingsScreen(
                searchHistoryCount = searchHistoryCount,
                onClearSearchHistory = viewModel::clearSearchHistory
            )
        }
        ListItem.PROVIDERS -> {
            /*TODO()*/
        }
        ListItem.APPEARANCE -> {
            /*TODO()*/
        }
        ListItem.PLAYER -> {
            /*TODO()*/
        }
        ListItem.DATA_AND_BACKUP -> {
            /*TODO()*/
        }
        ListItem.ADVANCED -> {
            /*TODO()*/
        }
        ListItem.ISSUE_A_BUG -> {
            /*TODO()*/
        }
        ListItem.FEATURE_REQUEST -> {
            /*TODO()*/
        }
        ListItem.REPOSITORY -> {
            /*TODO()*/
        }
    }
}

private fun getDialogKeys(): SnapshotStateMap<String, Boolean> {
    return mutableStateMapOf(
        KEY_PREFERRED_SERVER_DIALOG to false,
        KEY_SUBTITLE_LANGUAGE_DIALOG to false,
        KEY_SUBTITLE_COLOR_DIALOG to false,
        KEY_SUBTITLE_SIZE_DIALOG to false,
        KEY_SUBTITLE_FONT_STYLE_DIALOG to false,
        KEY_SUBTITLE_BACKGROUND_COLOR_DIALOG to false,
        KEY_SUBTITLE_EDGE_TYPE_DIALOG to false,
        KEY_PLAYER_SEEK_INCREMENT_MS_DIALOG to false,
        KEY_PLAYER_QUALITY_DIALOG to false,
        KEY_PLAYER_RESIZE_MODE_DIALOG to false,
        KEY_DOH_DIALOG to false,
        KEY_PLAYER_DISK_CACHE_DIALOG to false,
        KEY_PLAYER_BUFFER_SIZE_DIALOG to false,
        KEY_PLAYER_BUFFER_LENGTH_DIALOG to false,
        KEY_SEARCH_HISTORY_NOTICE_DIALOG to false,
        KEY_AUDIO_LANGUAGE_DIALOG to false,
        KEY_DECODER_PRIORITY_DIALOG to false,
    )
}

@Preview(device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
private fun TabletPreview() {
    FlixclusiveTheme {
        Surface {
            SettingsScreen()
        }
    }
}

@Preview(device = "spec:width=411dp,height=891dp")
@Composable
private fun PhonePreview() {
    FlixclusiveTheme {
        Surface {
            SettingsScreen()
        }
    }
}