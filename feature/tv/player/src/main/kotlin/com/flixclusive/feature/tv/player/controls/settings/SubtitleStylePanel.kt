package com.flixclusive.feature.tv.player.controls.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.NonInteractiveSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flixclusive.core.theme.subtitleBackgroundColors
import com.flixclusive.core.theme.subtitleColors
import com.flixclusive.core.ui.common.util.getTextStyle
import com.flixclusive.core.ui.common.util.ifElse
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.tv.util.focusOnInitialVisibility
import com.flixclusive.feature.tv.player.R
import com.flixclusive.feature.tv.player.controls.settings.common.BorderedText
import com.flixclusive.feature.tv.player.controls.settings.common.ConfirmButton
import com.flixclusive.model.datastore.AppSettings
import com.flixclusive.model.datastore.player.CaptionEdgeTypePreference
import com.flixclusive.model.datastore.player.CaptionSizePreference
import com.flixclusive.model.datastore.player.CaptionSizePreference.Companion.getDp
import com.flixclusive.model.datastore.player.CaptionStylePreference
import com.flixclusive.core.locale.R as LocaleR


private val styleItemSize = 20.dp

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun SubtitleStylePanel(
    modifier: Modifier = Modifier,
    appSettings: AppSettings,
    updateAppSettings: (AppSettings) -> Unit,
    hidePanel: () -> Unit,
) {
    var currentAppSettings by remember { mutableStateOf(appSettings) }

    fun updateToSaveAppSettings(appSettings: AppSettings) {
        currentAppSettings = appSettings
    }

    BackHandler {
        hidePanel()
    }

    Column(
        modifier = modifier
            .fillMaxWidth(0.8F)
            .padding(bottom = 50.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(id = LocaleR.string.sample_subtitle_text),
            style = currentAppSettings.subtitleFontStyle.getTextStyle().copy(
                color = Color(currentAppSettings.subtitleColor),
                fontSize = currentAppSettings.subtitleSize.getDp(isTv = true).sp,
                background = Color(currentAppSettings.subtitleBackgroundColor),
                shadow = Shadow(
                    offset = Offset(6F, 6F),
                    blurRadius = 3f,
                    color = Color(currentAppSettings.subtitleEdgeType.color),
                )
            )
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            modifier = Modifier
                .focusGroup()
                .padding(top = 20.dp)
        ) {
            SubtitleStyle(
                label = stringResource(LocaleR.string.subtitles_size)
            ) {
                CaptionSizePreference.entries.forEach {
                    StyleItem(
                        onClick = {
                            updateToSaveAppSettings(
                                currentAppSettings.copy(subtitleSize = it)
                            )
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.abc),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = it.getDp(isTv = true).times(0.5).sp
                            ),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                        )
                    }
                }
            }

            SubtitleStyle(
                label = stringResource(LocaleR.string.subtitles_edge_type)
            ) {
                CaptionEdgeTypePreference.entries.forEach {
                    StyleItem(
                        onClick = {
                            updateToSaveAppSettings(
                                currentAppSettings.copy(subtitleEdgeType = it)
                            )
                        }
                    ) {
                        when (it) {
                            CaptionEdgeTypePreference.Drop_Shadow -> {
                                Text(
                                    text = stringResource(R.string.abc),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontSize = 15.sp,
                                        shadow = Shadow(
                                            offset = Offset(4F, 4F),
                                            blurRadius = 2f,
                                            color = Color(currentAppSettings.subtitleEdgeType.color),
                                        )
                                    ),
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                )
                            }
                            CaptionEdgeTypePreference.Outline -> {
                                BorderedText(
                                    text = stringResource(R.string.abc),
                                    borderColor = Color(currentAppSettings.subtitleEdgeType.color),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontSize = 15.sp,
                                        textAlign = TextAlign.Center
                                    ),
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
            }

            SubtitleStyle(
                label = stringResource(LocaleR.string.subtitles_font_style)
            ) {
                CaptionStylePreference.entries.forEach {
                    StyleItem(
                        onClick = {
                            updateToSaveAppSettings(
                                currentAppSettings.copy(subtitleFontStyle = it)
                            )
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.abc),
                            style = it.getTextStyle().copy(
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier
                                .align(Alignment.Center)
                        )
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            modifier = Modifier
                .focusGroup()
        ) {
            SubtitleStyle(
                label = stringResource(LocaleR.string.subtitles_color)
            ) {
                subtitleColors.forEach { (_, color) ->
                    StyleItem(
                        onClick = {
                            updateToSaveAppSettings(
                                currentAppSettings.copy(subtitleColor = color.toArgb())
                            )
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(color)
                                .align(Alignment.Center)
                        )
                    }
                }
            }

            SubtitleStyle(
                label = stringResource(LocaleR.string.subtitles_background_color)
            ) {
                subtitleBackgroundColors.forEach { (_, color) ->
                    StyleItem(
                        onClick = {
                            updateToSaveAppSettings(
                                currentAppSettings.copy(subtitleBackgroundColor = color.toArgb())
                            )
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .align(Alignment.Center)
                                .background(color)
                                .ifElse(
                                    condition = color == Color.Transparent,
                                    ifTrueModifier = Modifier.border(
                                        width = if (color == Color.Transparent) 1.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        shape = MaterialTheme.shapes.extraSmall
                                    )
                                )
                        )
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            modifier = Modifier
                .padding(top = 20.dp)
                .focusGroup()
        ) {
            ConfirmButton(
                onClick = {
                    updateAppSettings(currentAppSettings)
                    hidePanel()
                },
                isEmphasis = true,
                label = stringResource(id = LocaleR.string.save)
            )

            ConfirmButton(
                onClick = hidePanel,
                isEmphasis = false,
                label = stringResource(id = LocaleR.string.cancel),
                modifier = Modifier
                    .focusOnInitialVisibility()
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SubtitleStyle(
    label: String,
    content: @Composable RowScope.() -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.15F else 1F,
        label = ""
    )

    Column(
        modifier = Modifier
            .focusGroup()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .padding(bottom = 3.dp)
        )

        Surface(
            tonalElevation = 2.dp,
            colors = NonInteractiveSurfaceDefaults.colors(
                containerColor = Color.Gray.onMediumEmphasis()
            ),
            shape = MaterialTheme.shapes.extraSmall,
            modifier = Modifier
                .onFocusChanged { isFocused = it.hasFocus }
                .height(30.dp)
                .scale(scale)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(5.dp)
                    .fillMaxHeight()
            ) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun StyleItem(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        modifier = modifier.sizeIn(minWidth = styleItemSize, minHeight = styleItemSize),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = LocalContentColor.current,
            focusedContainerColor = Color.Transparent,
            focusedContentColor = LocalContentColor.current,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                ),
            )
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1F),
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.extraSmall),
        onClick = onClick
    ) {
        content()
    }
}