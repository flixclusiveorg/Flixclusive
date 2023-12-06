package com.flixclusive.presentation.mobile.screens.player.controls.audio_and_display_sheet

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flixclusive.R
import com.flixclusive.presentation.mobile.screens.player.controls.common.SheetItem
import com.flixclusive.presentation.mobile.screens.player.controls.common.SheetItemPlaceholder
import com.flixclusive.presentation.utils.ModifierUtils.fadingEdge
import com.flixclusive.providers.models.common.Subtitle

@SuppressLint("OpaqueUnitKey")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class,
    ExperimentalAnimationApi::class
)
@Composable
fun AnimatedVisibilityScope.AudioAndDisplaySheet(
    modifier: Modifier = Modifier,
    subtitles: List<Subtitle>,
    qualities: List<String>,
    audios: List<String>,
    selectedSubtitle: Int,
    selectedQuality: Int,
    selectedAudio: Int,
    onSubtitleChange: (Int, String) -> Unit,
    onVideoQualityChange: (Int, String) -> Unit,
    onAudioChange: (Int, String) -> Unit,
    onDismissSheet: () -> Unit,
) {
    val listBottomFade =
        remember { Brush.verticalGradient(0.8f to Color.Red, 0.96f to Color.Transparent) }

    val dismissState = rememberDismissState(
        confirmValueChange = {
            onDismissSheet()
            true
        }
    )

    DisposableEffect(
        Box(
            modifier = modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismissSheet() },
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0F to Color.Transparent,
                            0.85F to Color.Black
                        )
                    )
                    .animateEnterExit(
                        fadeIn(),
                        fadeOut()
                    )
            )

            SwipeToDismiss(
                state = dismissState,
                background = {},
                dismissContent = {
                    Box(
                        modifier = Modifier
                            .width(300.dp)
                            .fillMaxHeight()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { /*Do nothing*/ }
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fadingEdge(listBottomFade)
                                .fillMaxHeight()
                        ) {
                            stickyHeader {
                                DismissSheetButton(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    onDismissIconClick = onDismissSheet
                                )
                            }

                            item {
                                Text(
                                    text = stringResource(R.string.quality),
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(start = 20.dp, top = 20.dp),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            if(qualities.isEmpty()) {
                                item {
                                    SheetItemPlaceholder()
                                }
                            }
                            else {
                                itemsIndexed(
                                    items = qualities,
                                    key = { _, quality -> quality }
                                ) { i, quality ->
                                    SheetItem(
                                        name = quality,
                                        index = i,
                                        selectedIndex = selectedQuality,
                                        onClick = {
                                            onVideoQualityChange(i, quality)
                                            onDismissSheet()
                                        }
                                    )
                                }
                            }

                            if(audios.isNotEmpty()) {
                                item {
                                    Text(
                                        text = stringResource(R.string.audio),
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.padding(start = 20.dp, top = 20.dp),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                itemsIndexed(
                                    items = audios,
                                    key = { _, audio -> audio }
                                ) { i, audio ->
                                    SheetItem(
                                        name = audio,
                                        index = i,
                                        selectedIndex = selectedAudio,
                                        onClick = {
                                            onAudioChange(i, audio)
                                            onDismissSheet()
                                        }
                                    )
                                }
                            }

                            item {
                                Text(
                                    text = stringResource(R.string.subtitle) + " - [${subtitles.size}]",
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(start = 20.dp, top = 20.dp),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            itemsIndexed(
                                items = subtitles,
                                key = { _, subtitle -> subtitle }
                            ) { i, subtitle ->
                                SheetItem(
                                    name = subtitle.lang,
                                    index = i,
                                    selectedIndex = selectedSubtitle,
                                    onClick = {
                                        onSubtitleChange(i, subtitle.lang)
                                        onDismissSheet()
                                    }
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                },
                directions = setOf(DismissDirection.StartToEnd)
            )
        }
    ) {
        onDispose {
            onDismissSheet()
        }
    }
}


