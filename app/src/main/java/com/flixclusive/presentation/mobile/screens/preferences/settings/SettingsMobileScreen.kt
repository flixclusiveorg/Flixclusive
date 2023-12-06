package com.flixclusive.presentation.mobile.screens.preferences.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.R
import com.flixclusive.domain.preferences.AppSettings
import com.flixclusive.domain.preferences.CaptionEdgeTypePreference
import com.flixclusive.domain.preferences.CaptionStylePreference
import com.flixclusive.presentation.common.FadeInAndOutScreenTransition
import com.flixclusive.presentation.mobile.main.LABEL_START_PADDING
import com.flixclusive.presentation.mobile.screens.preferences.PreferencesNavGraph
import com.flixclusive.presentation.mobile.screens.preferences.common.TopBarWithNavigationIcon
import com.flixclusive.presentation.mobile.screens.preferences.settings.dialog_groups.subtitles.SubtitleDialogWrapper
import com.flixclusive.presentation.mobile.screens.preferences.settings.dialog_groups.subtitles.SubtitlePreview
import com.flixclusive.presentation.mobile.screens.preferences.settings.dialog_groups.video_player.VideoPlayerDialogWrapper
import com.flixclusive.presentation.mobile.utils.ComposeMobileUtils.colorOnMediumEmphasisMobile
import com.flixclusive.presentation.utils.ColorPickerUtils.BoxWithColor
import com.flixclusive.presentation.utils.ComposeUtils.BorderedText
import com.flixclusive.presentation.utils.LazyListUtils.isAtTop
import com.flixclusive.presentation.utils.LazyListUtils.isScrollingUp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import java.util.Locale

private const val DEFAULT_TEXT_PREVIEW = "Abc"
private val settingItemShape = RoundedCornerShape(20.dp)

@PreferencesNavGraph
@Destination(
    style = FadeInAndOutScreenTransition::class
)
@Composable
fun SettingsMobileScreen(
    navigator: DestinationsNavigator,
) {
    val viewModel = hiltViewModel<SettingsMobileScreenViewModel>()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()

    val toggleSubtitlesLabel = stringResource(R.string.subtitle)
    val toggleFilmCardTitleLabel = stringResource(R.string.film_card_titles)

    val toggleSwitch = { newValue: AppSettings ->
        viewModel.onChangeSettings(newValue)
    }

    val currentGeneralSettings = listOf(
        SettingsItem(
            title = toggleFilmCardTitleLabel,
            description = "Show titles on film cards",
            previewContent = {
                Switch(
                    checked = appSettings.isShowingFilmCardTitle,
                    onCheckedChange = {
                        toggleSwitch(appSettings.copy(isShowingFilmCardTitle = it))
                    },
                    modifier = Modifier.scale(0.7F)
                )
            },
        ),
    )

    val currentSubtitlesSettings = listOf(
        SettingsItem(
            title = toggleSubtitlesLabel,
            description = "Toggle to turn subtitles on/off",
            previewContent = {
                Switch(
                    checked = appSettings.isSubtitleEnabled,
                    onCheckedChange = {
                        toggleSwitch(appSettings.copy(isShowingFilmCardTitle = it))
                    },
                    modifier = Modifier.scale(0.7F)
                )
            }
        ),
        SettingsItem(
            title = stringResource(R.string.subtitles_language),
            description = Locale(appSettings.subtitleLanguage).displayLanguage,
            enabled = appSettings.isSubtitleEnabled,
            dialogKey = KEY_SUBTITLE_LANGUAGE_DIALOG,
        ),
        SettingsItem(
            title = stringResource(R.string.subtitles_size),
            description = appSettings.subtitleSize.toString(),
            enabled = appSettings.isSubtitleEnabled,
            dialogKey = KEY_SUBTITLE_SIZE_DIALOG
        ),
        SettingsItem(
            title = stringResource(R.string.subtitles_font_style),
            description = appSettings.subtitleFontStyle.toString(),
            enabled = appSettings.isSubtitleEnabled,
            dialogKey = KEY_SUBTITLE_FONT_STYLE_DIALOG,
            previewContent = {
                Text(
                    text = DEFAULT_TEXT_PREVIEW,
                    style = when (appSettings.subtitleFontStyle) {
                        CaptionStylePreference.Normal -> MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Normal
                        )

                        CaptionStylePreference.Bold -> MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        )

                        CaptionStylePreference.Italic -> MaterialTheme.typography.labelLarge.copy(
                            fontStyle = FontStyle.Italic
                        )

                        CaptionStylePreference.Monospace -> MaterialTheme.typography.labelLarge.copy(
                            fontFamily = FontFamily.Monospace
                        )
                    }
                )
            }
        ),
        SettingsItem(
            title = stringResource(R.string.subtitles_color),
            description = "Choose a readable and preferred color",
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
            title = stringResource(R.string.subtitles_background_color),
            description = "Choose a background color behind the subtitle",
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
            title = stringResource(R.string.subtitles_edge_type),
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
            title = stringResource(R.string.quality),
            description = appSettings.preferredQuality,
            dialogKey = KEY_VIDEO_PLAYER_QUALITY_DIALOG,
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
                TopBarWithNavigationIcon(
                    headerTitle = stringResource(id = R.string.settings),
                    onNavigationIconClick = navigator::navigateUp
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
                    onItemClick = {
                        toggleSwitch(appSettings.copy(isShowingFilmCardTitle = !appSettings.isShowingFilmCardTitle))
                    }
                )
            }

            item {
                SettingsGroup(
                    items = currentVideoPlayerSettings,
                    onItemClick = { item ->
                        viewModel.toggleDialog(item.dialogKey!!)
                    }
                )
            }

            item {
                SettingsGroup(
                    items = currentSubtitlesSettings,
                    onItemClick = { item ->
                        if (item.dialogKey != null) {
                            viewModel.toggleDialog(item.dialogKey)
                        } else {
                            toggleSwitch(appSettings.copy(isShowingFilmCardTitle = !appSettings.isSubtitleEnabled))
                        }
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

    VideoPlayerDialogWrapper(
        openedDialogMap = viewModel.openedDialogMap,
        appSettings = appSettings,
        onChange = viewModel::onChangeSettings,
        onDismissDialog = viewModel::toggleDialog
    )
}


@Composable
private fun SettingsGroup(
    items: List<SettingsItem>,
    onItemClick: (SettingsItem) -> Unit = {},
) {
    Surface(
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.background,
        shape = settingItemShape
    ) {
        Column {
            Spacer(modifier = Modifier.padding(vertical = 5.dp))

            items.forEachIndexed { i, item ->
                SettingsGroupItem(
                    title = item.title,
                    description = item.description?.replace("_", " "),
                    previewContent = item.previewContent,
                    enabled = item.enabled,
                    onClick = {
                        onItemClick(item)
                    }
                )

                if (i < items.lastIndex)
                    Divider(
                        color = colorOnMediumEmphasisMobile(emphasis = 0.3F),
                        thickness = 1.dp,
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                    )
            }

            Spacer(modifier = Modifier.padding(vertical = 5.dp))
        }
    }
}

@Composable
private fun SettingsGroupItem(
    title: String,
    description: String?,
    enabled: Boolean,
    previewContent: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    val color by animateColorAsState(
        targetValue = if (enabled) Color.White else Color.Gray,
        label = ""
    )

    CompositionLocalProvider(
        LocalContentColor provides color
    ) {
        Box(
            modifier = Modifier
                .clickable(enabled = enabled) {
                    onClick()
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .padding(horizontal = LABEL_START_PADDING),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(
                        space = 3.dp,
                        alignment = Alignment.CenterVertically
                    ),
                    modifier = Modifier
                        .weight(1F)
                        .padding(end = LABEL_START_PADDING)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Normal,
                            fontSize = 16.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )

                    description?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = colorOnMediumEmphasisMobile(LocalContentColor.current)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                previewContent()
            }
        }
    }
}