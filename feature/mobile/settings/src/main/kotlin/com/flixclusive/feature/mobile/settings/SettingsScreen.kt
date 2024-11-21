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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.user.getAvatarResource
import com.flixclusive.feature.mobile.settings.screen.general.GeneralSettingsScreen
import com.flixclusive.model.database.User

@SuppressLint("UnusedContentLambdaTargetStateParameter")
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun SettingsScreen() {
    val currentUser = remember { User() }

    val navigator = rememberListDetailPaneScaffoldNavigator<ListItem>()
    val isListAndDetailVisible =
        navigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Expanded
        && navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded

    val backgroundBrush = getAdaptiveBackground(currentUser)

    BackHandler(navigator.canNavigateBack()) {
        navigator.navigateBack()
    }

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
                        currentUser = { currentUser },
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
                                NavigatedScreen(listItem = item)
                            }
                        }
                    )
                }
            }
        )
    }
}

@Composable
private fun getAdaptiveBackground(
    currentUser: User,
): Brush {
    val context = LocalContext.current

    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary

    return remember(currentUser.image) {
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

        val color = swatch?.rgb?.let { Color(it) }
            ?: primaryColor

        Brush.verticalGradient(
            0F to color.copy(0.3F),
            0.4F to surfaceColor
        )
    }
}

@Composable
private fun NavigatedScreen(
    listItem: ListItem
) {
    when (listItem) {
        ListItem.GENERAL_SETTINGS -> GeneralSettingsScreen()
        ListItem.PROVIDERS -> TODO()
        ListItem.APPEARANCE -> TODO()
        ListItem.PLAYER -> TODO()
        ListItem.DATA_AND_BACKUP -> TODO()
        ListItem.ADVANCED -> TODO()
        ListItem.ISSUE_A_BUG -> TODO()
        ListItem.FEATURE_REQUEST -> TODO()
        ListItem.REPOSITORY -> TODO()
    }
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