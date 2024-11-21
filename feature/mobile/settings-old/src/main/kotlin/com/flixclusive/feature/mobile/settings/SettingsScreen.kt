package com.flixclusive.feature.mobile.settings

import android.text.format.Formatter.formatShortFileSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.ui.common.CommonTopBar
import com.flixclusive.core.ui.common.navigation.GoBackAction
import com.flixclusive.core.ui.mobile.util.isAtTop
import com.flixclusive.core.ui.mobile.util.isScrollingUp
import com.flixclusive.core.util.android.getDirectorySize
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.feature.mobile.settings.component.EditUserAgentDialog
import com.flixclusive.feature.mobile.settings.component.PreReleaseUpdatesWarningDialog
import com.flixclusive.feature.mobile.settings.component.SettingsGroup
import com.flixclusive.feature.mobile.settings.component.dialog.advanced.AdvancedDialogWrapper
import com.flixclusive.feature.mobile.settings.component.dialog.general.GeneralDialogWrapper
import com.flixclusive.feature.mobile.settings.component.dialog.player.PlayerDialogWrapper
import com.flixclusive.feature.mobile.settings.component.dialog.subtitles.SubtitleDialogWrapper
import com.flixclusive.feature.mobile.settings.component.dialog.subtitles.SubtitlePreview
import com.flixclusive.feature.mobile.settings.settings_group.currentAdvancedPlayerSettings
import com.flixclusive.feature.mobile.settings.settings_group.currentGeneralSettings
import com.flixclusive.feature.mobile.settings.settings_group.currentNetworkSettings
import com.flixclusive.feature.mobile.settings.settings_group.currentProviderSettings
import com.flixclusive.feature.mobile.settings.settings_group.currentSubtitlesSettings
import com.flixclusive.feature.mobile.settings.settings_group.currentUiSettings
import com.flixclusive.feature.mobile.settings.settings_group.currentVideoPlayerSettings
import com.flixclusive.feature.mobile.settings.util.AppSettingsHelper.LocalAppSettings
import com.flixclusive.feature.mobile.settings.util.AppSettingsHelper.LocalAppSettingsChanger
import com.flixclusive.feature.mobile.settings.util.ProviderSettingsHelper.LocalAppSettingsProvider
import com.flixclusive.feature.mobile.settings.util.ProviderSettingsHelper.LocalAppSettingsProviderChanger
import com.ramcosta.composedestinations.annotation.Destination
import com.flixclusive.core.locale.R as LocaleR

internal val settingItemShape = RoundedCornerShape(20.dp)

@Destination
@Composable
internal fun SettingsScreen(
    navigator: GoBackAction,
) {
    val context = LocalContext.current

    val viewModel = hiltViewModel<SettingsScreenViewModel>()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val appSettingsProvider by viewModel.appSettingsProvider.collectAsStateWithLifecycle()
    val searchHistoryCount by viewModel.searchHistoryCount.collectAsStateWithLifecycle()

    var sizeSummary: String? by remember { mutableStateOf(null) }
    val (isOptingInOnPreReleaseUpdates, onUsePrereleaseUpdatesChange)
        = rememberSaveable { mutableStateOf(false) }
    var isUserAgentDialogOpen by rememberSaveable { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val shouldShowTopBar by listState.isScrollingUp()
    val listIsAtTop by listState.isAtTop()

    fun updateAppCacheSize() {
        sizeSummary = safeCall {
            formatShortFileSize(
                /* context = */ context,
                /* sizeBytes = */ getDirectorySize(context.cacheDir)
            )
        }
    }

    val onSettingsItemClick = { item: SettingsItem ->
        when {
            item.onClick != null -> item.onClick.invoke()
            else -> viewModel.toggleDialog(item.dialogKey!!)
        }
    }

    LaunchedEffect(Unit) {
        updateAppCacheSize()
    }


    CompositionLocalProvider(
        LocalAppSettings provides appSettings,
        LocalAppSettingsChanger provides viewModel::onChangeAppSettings,
        LocalAppSettingsProvider provides appSettingsProvider,
        LocalAppSettingsProviderChanger provides viewModel::onChangeAppSettingsProvider,
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize(),
            topBar = {
                AnimatedVisibility(
                    visible = shouldShowTopBar,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                ) {
                    CommonTopBar(
                        headerTitle = stringResource(id = LocaleR.string.settings),
                        onNavigationIconClick = navigator::goBack
                    )
                }
            }
        ) { innerPadding ->
            val topPadding by animateDpAsState(
                targetValue = if (listIsAtTop) innerPadding.calculateTopPadding() else 0.dp,
                label = ""
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.padding(top = topPadding),
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                item {
                    SettingsGroup(
                        items = currentGeneralSettings(
                            searchHistoryCount = searchHistoryCount,
                            onUsePrereleaseUpdatesChange = onUsePrereleaseUpdatesChange
                        ),
                        onItemClick = onSettingsItemClick
                    )
                }

                item {
                    SettingsGroup(
                        items = currentProviderSettings(),
                        onItemClick = onSettingsItemClick
                    )
                }

                item {
                    SettingsGroup(
                        items = currentUiSettings(),
                        onItemClick = onSettingsItemClick
                    )
                }

                item {
                    SettingsGroup(
                        items = currentVideoPlayerSettings(
                            cacheLinksSize = viewModel.cacheLinksSize,
                            clearCacheLinks = viewModel::clearCacheLinks
                        ),
                        onItemClick = onSettingsItemClick
                    )
                }

                item {
                    SettingsGroup(
                        items = currentSubtitlesSettings(),
                        onItemClick = onSettingsItemClick
                    )
                }

                item {
                    Surface(
                        tonalElevation = 3.dp,
                        color = MaterialTheme.colorScheme.background,
                        shape = settingItemShape,
                    ) {
                        SubtitlePreview(
                            appSettings = appSettings,
                            shape = settingItemShape
                        )
                    }
                }

                item {
                    SettingsGroup(
                        items = currentNetworkSettings(
                            onOpenUserAgentDialog = { isUserAgentDialogOpen = true }
                        ),
                        onItemClick = onSettingsItemClick
                    )
                }

                item {
                    SettingsGroup(
                        items = currentAdvancedPlayerSettings(
                            sizeSummary = sizeSummary,
                            updateAppCacheSize = { updateAppCacheSize() }
                        ),
                        onItemClick = onSettingsItemClick
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(25.dp))
                }
            }
        }

        GeneralDialogWrapper(viewModel)

        SubtitleDialogWrapper(
            openedDialogMap = viewModel.openedDialogMap,
            onDismissDialog = viewModel::toggleDialog
        )

        PlayerDialogWrapper(
            openedDialogMap = viewModel.openedDialogMap,
            onDismissDialog = viewModel::toggleDialog
        )

        AdvancedDialogWrapper(
            openedDialogMap = viewModel.openedDialogMap,
            onDismissDialog = viewModel::toggleDialog
        )
    }


    if (isOptingInOnPreReleaseUpdates) {
        PreReleaseUpdatesWarningDialog(
            onConfirm = {
                viewModel.onChangeAppSettings(appSettings.copy(isUsingPrereleaseUpdates = true))
                onUsePrereleaseUpdatesChange(false)
            },
            onDismiss = { onUsePrereleaseUpdatesChange(false) }
        )
    }

    if (isUserAgentDialogOpen) {
        EditUserAgentDialog(
            defaultUserAgent = appSettings.userAgent,
            onDismiss = { isUserAgentDialogOpen = false },
            onConfirm = {
                if (it.equals(appSettings.userAgent, ignoreCase = true))
                    return@EditUserAgentDialog

                viewModel.onChangeAppSettings(appSettings.copy(userAgent = it))
            }
        )
    }
}




