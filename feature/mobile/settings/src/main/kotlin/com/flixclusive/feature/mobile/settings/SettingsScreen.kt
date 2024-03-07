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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.ui.common.CommonTopBar
import com.flixclusive.core.ui.common.navigation.GoBackAction
import com.flixclusive.core.ui.mobile.util.isAtTop
import com.flixclusive.core.ui.mobile.util.isScrollingUp
import com.flixclusive.core.util.android.getDirectorySize
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.feature.mobile.settings.component.SettingsGroup
import com.flixclusive.feature.mobile.settings.component.dialog.advanced.AdvancedDialogWrapper
import com.flixclusive.feature.mobile.settings.component.dialog.player.PlayerDialogWrapper
import com.flixclusive.feature.mobile.settings.component.dialog.subtitles.BorderedText
import com.flixclusive.feature.mobile.settings.component.dialog.subtitles.SubtitleDialogWrapper
import com.flixclusive.feature.mobile.settings.component.dialog.subtitles.SubtitlePreview
import com.flixclusive.feature.mobile.settings.util.ColorPickerHelper.BoxWithColor
import com.flixclusive.model.datastore.player.CaptionEdgeTypePreference
import com.flixclusive.model.datastore.player.CaptionStylePreference
import com.flixclusive.model.datastore.player.ResizeMode
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.launch
import java.util.Locale
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.util.R as UtilR

private const val DEFAULT_TEXT_PREVIEW = "Abc"
internal val settingItemShape = RoundedCornerShape(20.dp)

@Destination
@Composable
fun SettingsScreen(
    navigator: GoBackAction,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val viewModel = hiltViewModel<SettingsScreenViewModel>()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()

    var sizeSummary: String? by remember { mutableStateOf(null) }

    fun updateAppCacheSize() {
        sizeSummary = safeCall {
            formatShortFileSize(
                /* context = */ context,
                /* sizeBytes = */ getDirectorySize(context.cacheDir)
            )
        }
    }

    LaunchedEffect(Unit) {
        updateAppCacheSize()
    }

    val currentGeneralSettings = listOf(
        SettingsItem(
            title = stringResource(UtilR.string.film_card_titles),
            description = stringResource(UtilR.string.film_card_titles_label),
            onClick = {
                viewModel.onChangeSettings(appSettings.copy(isShowingFilmCardTitle = !appSettings.isShowingFilmCardTitle))
            },
            previewContent = {
                Switch(
                    checked = appSettings.isShowingFilmCardTitle,
                    onCheckedChange = {
                        viewModel.onChangeSettings(appSettings.copy(isShowingFilmCardTitle = it))
                    },
                    modifier = Modifier.scale(0.7F)
                )
            },
        ),
        SettingsItem(
            title = stringResource(UtilR.string.automatic_crash_report),
            description = stringResource(UtilR.string.automatic_crash_report_label),
            onClick = {
                viewModel.onChangeSettings(appSettings.copy(isSendingCrashLogsAutomatically = !appSettings.isSendingCrashLogsAutomatically))
            },
            previewContent = {
                Switch(
                    checked = appSettings.isSendingCrashLogsAutomatically,
                    onCheckedChange = {
                        viewModel.onChangeSettings(appSettings.copy(isSendingCrashLogsAutomatically = it))
                    },
                    modifier = Modifier.scale(0.7F)
                )
            },
        ),
    )

    val currentSubtitlesSettings = listOf(
        SettingsItem(
            title = stringResource(UtilR.string.subtitle),
            description = stringResource(id = UtilR.string.subtitles_toggle_desc),
            onClick = {
                viewModel.onChangeSettings(appSettings.copy(isSubtitleEnabled = !appSettings.isSubtitleEnabled))
            },
            previewContent = {
                Switch(
                    checked = appSettings.isSubtitleEnabled,
                    onCheckedChange = {
                        viewModel.onChangeSettings(appSettings.copy(isSubtitleEnabled = it))
                    },
                    modifier = Modifier.scale(0.7F)
                )
            }
        ),
        SettingsItem(
            title = stringResource(UtilR.string.subtitles_language),
            description = Locale(appSettings.subtitleLanguage).displayLanguage,
            enabled = appSettings.isSubtitleEnabled,
            dialogKey = KEY_SUBTITLE_LANGUAGE_DIALOG,
        ),
        SettingsItem(
            title = stringResource(UtilR.string.subtitles_size),
            description = appSettings.subtitleSize.toString(),
            enabled = appSettings.isSubtitleEnabled,
            dialogKey = KEY_SUBTITLE_SIZE_DIALOG
        ),
        SettingsItem(
            title = stringResource(UtilR.string.subtitles_font_style),
            description = appSettings.subtitleFontStyle.toString(),
            enabled = appSettings.isSubtitleEnabled,
            dialogKey = KEY_SUBTITLE_FONT_STYLE_DIALOG,
            previewContent = {
                Text(
                    text = DEFAULT_TEXT_PREVIEW,
                    style = MaterialTheme.typography.labelLarge.run {
                        when (appSettings.subtitleFontStyle) {
                            CaptionStylePreference.Normal -> copy(
                                fontWeight = FontWeight.Normal
                            )

                            CaptionStylePreference.Bold -> copy(
                                fontWeight = FontWeight.Bold
                            )

                            CaptionStylePreference.Italic -> copy(
                                fontStyle = FontStyle.Italic
                            )

                            CaptionStylePreference.Monospace -> copy(
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                )
            }
        ),
        SettingsItem(
            title = stringResource(UtilR.string.subtitles_color),
            description = stringResource(UtilR.string.subtitles_color_desc),
            enabled = appSettings.isSubtitleEnabled,
            dialogKey = KEY_SUBTITLE_COLOR_DIALOG,
            previewContent = {
                BoxWithColor(
                    if (appSettings.isSubtitleEnabled) appSettings.subtitleColor
                    else Color.Gray.toArgb()
                )
            }
        ),
        SettingsItem(
            title = stringResource(UtilR.string.subtitles_background_color),
            description = stringResource(UtilR.string.subtitles_background_color_desc),
            enabled = appSettings.isSubtitleEnabled,
            dialogKey = KEY_SUBTITLE_BACKGROUND_COLOR_DIALOG,
            previewContent = {
                BoxWithColor(
                    if (appSettings.isSubtitleEnabled) appSettings.subtitleBackgroundColor
                    else Color.Gray.toArgb()
                )
            }
        ),
        SettingsItem(
            title = stringResource(UtilR.string.subtitles_edge_type),
            description = appSettings.subtitleEdgeType.toString(),
            enabled = appSettings.isSubtitleEnabled,
            dialogKey = KEY_SUBTITLE_EDGE_TYPE_DIALOG,
            previewContent = {
                when (appSettings.subtitleEdgeType) {
                    CaptionEdgeTypePreference.Drop_Shadow -> {
                        Text(
                            text = DEFAULT_TEXT_PREVIEW,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Medium,
                                shadow = Shadow(
                                    offset = Offset(6F, 6F),
                                    blurRadius = 3f,
                                    color = Color(appSettings.subtitleEdgeType.color),
                                ),
                            )
                        )
                    }

                    CaptionEdgeTypePreference.Outline -> {
                        BorderedText(
                            text = DEFAULT_TEXT_PREVIEW,
                            borderColor = Color(appSettings.subtitleEdgeType.color),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Medium,
                            )
                        )
                    }
                }
            }
        ),
    )

    val currentVideoPlayerSettings = listOf(
        SettingsItem(
            title = stringResource(UtilR.string.release_player),
            description = stringResource(UtilR.string.release_player_desc),
            onClick = {
                viewModel.onChangeSettings(appSettings.copy(shouldReleasePlayer = !appSettings.shouldReleasePlayer))
            },
            previewContent = {
                Switch(
                    checked = appSettings.shouldReleasePlayer,
                    onCheckedChange = {
                        viewModel.onChangeSettings(appSettings.copy(shouldReleasePlayer = it))
                    },
                    modifier = Modifier.scale(0.7F)
                )
            }
        ),
        SettingsItem(
            title = stringResource(UtilR.string.reverse_player_time),
            onClick = {
                viewModel.onChangeSettings(appSettings.copy(isPlayerTimeReversed = !appSettings.isPlayerTimeReversed))
            },
            previewContent = {
                Switch(
                    checked = appSettings.isPlayerTimeReversed,
                    onCheckedChange = {
                        viewModel.onChangeSettings(appSettings.copy(isPlayerTimeReversed = !appSettings.isPlayerTimeReversed))
                    },
                    modifier = Modifier.scale(0.7F)
                )
            }
        ),
        SettingsItem(
            title = stringResource(UtilR.string.preferred_quality),
            description = appSettings.preferredQuality.qualityName,
            dialogKey = KEY_PLAYER_QUALITY_DIALOG,
        ),
        SettingsItem(
            title = stringResource(UtilR.string.preferred_resize_mode),
            description = ResizeMode.entries.find { it.mode == appSettings.preferredResizeMode }.toString(),
            dialogKey = KEY_PLAYER_RESIZE_MODE_DIALOG,
        ),
        SettingsItem(
            title = stringResource(UtilR.string.seek_length_label),
            description = "${appSettings.preferredSeekAmount / 1000} seconds",
            dialogKey = KEY_PLAYER_SEEK_INCREMENT_MS_DIALOG,
        ),
    )

    val currentAdvancedSettings = listOf(
        SettingsItem(
            title = stringResource(UtilR.string.doh),
            description = stringResource(UtilR.string.dns_label),
            dialogKey = KEY_DOH_DIALOG,
        ),
    )

    val currentCacheSettings = listOf(
        SettingsItem(
            title = stringResource(UtilR.string.video_cache_size),
            description = stringResource(UtilR.string.video_cache_size_label),
            dialogKey = KEY_PLAYER_DISK_CACHE_DIALOG,
        ),
        SettingsItem(
            title = stringResource(UtilR.string.video_buffer_size),
            description = stringResource(UtilR.string.video_buffer_size_label),
            dialogKey = KEY_PLAYER_BUFFER_SIZE_DIALOG,
        ),
        SettingsItem(
            title = stringResource(UtilR.string.video_buffer_max_length),
            description = stringResource(UtilR.string.video_buffer_max_length_desc),
            dialogKey = KEY_PLAYER_BUFFER_LENGTH_DIALOG,
        ),
        SettingsItem(
            title = stringResource(UtilR.string.clear_app_cache),
            description = sizeSummary,
            onClick = {
                safeCall {
                    scope.launch { context.cacheDir.deleteRecursively() }
                    updateAppCacheSize()
                }
            },
            previewContent = {
                Icon(
                    painter = painterResource(id = UiCommonR.drawable.delete),
                    contentDescription = stringResource(
                        id = UtilR.string.clear_cache_content_desc
                    )
                )
            }
        ),
    )

    val listState = rememberLazyListState()
    val shouldShowTopBar by listState.isScrollingUp()
    val listIsAtTop by listState.isAtTop()

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
                    headerTitle = stringResource(id = UtilR.string.settings),
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
                    items = currentGeneralSettings,
                    onItemClick = { item ->
                        if(item.onClick != null) {
                            item.onClick.invoke()
                        } else viewModel.toggleDialog(item.dialogKey!!)
                    }
                )
            }

            item {
                SettingsGroup(
                    items = currentVideoPlayerSettings,
                    onItemClick = { item ->
                        if(item.onClick != null) {
                            item.onClick.invoke()
                        } else viewModel.toggleDialog(item.dialogKey!!)
                    }
                )
            }

            item {
                SettingsGroup(
                    items = currentSubtitlesSettings,
                    onItemClick = { item ->
                        if(item.onClick != null) {
                            item.onClick.invoke()
                        } else viewModel.toggleDialog(item.dialogKey!!)
                    }
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
                    items = currentAdvancedSettings,
                    onItemClick = { item ->
                        viewModel.toggleDialog(item.dialogKey!!)
                    }
                )
            }

            item {
                SettingsGroup(
                    items = currentCacheSettings,
                    onItemClick = { item ->
                        if(item.onClick != null) {
                            item.onClick.invoke()
                        } else viewModel.toggleDialog(item.dialogKey!!)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(25.dp))
            }
        }
    }

    SubtitleDialogWrapper(
        openedDialogMap = viewModel.openedDialogMap,
        appSettings = appSettings,
        onChange = viewModel::onChangeSettings,
        onDismissDialog = viewModel::toggleDialog
    )

    PlayerDialogWrapper(
        openedDialogMap = viewModel.openedDialogMap,
        appSettings = appSettings,
        onChange = viewModel::onChangeSettings,
        onDismissDialog = viewModel::toggleDialog
    )

    AdvancedDialogWrapper(
        openedDialogMap = viewModel.openedDialogMap,
        appSettings = appSettings,
        onChange = viewModel::onChangeSettings,
        onDismissDialog = viewModel::toggleDialog
    )
}




