@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.flixclusive.feature.tv.film.component.episodes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.imageLoader
import com.flixclusive.core.ui.common.util.buildImageUrl
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.common.util.placeholderEffect
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun EpisodeCard(
    modifier: Modifier = Modifier,
    episode: Episode,
    onEpisodeClick: () -> Unit,
) {
    val context = LocalContext.current

    val shape = MaterialTheme.shapes.extraSmall
    var isFocused by remember { mutableStateOf(false) }
    val borderFocused = remember(isFocused) {
        if (isFocused) {
            BorderStroke(width = 2.dp, color = Color.White)
        } else BorderStroke(width = 0.dp, color = Color.Transparent)
    }

    Surface(
        onClick = onEpisodeClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = LocalContentColor.current.onMediumEmphasis(),
            focusedContainerColor = Color.Transparent,
            focusedContentColor = Color.White,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03F),
        modifier = modifier
            .onFocusChanged {
                isFocused = it.isFocused
            }
    ) {
        Row(
            modifier = Modifier
                .height(130.dp)
                .padding(horizontal = 10.dp)
                .background(
                    Color.Transparent,
                    MaterialTheme.shapes.extraSmall
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(180.dp)
                    .padding(10.dp)
                    .graphicsLayer {
                        this.shape = shape
                        clip = true
                    }
                    .border(borderFocused)
            ) {
                AsyncImage(
                    model = context.buildImageUrl(
                        imagePath = episode.image,
                        imageSize = "w500"
                    ),
                    imageLoader = LocalContext.current.imageLoader,
                    contentDescription = stringResource(
                        LocaleR.string.episode_image_content_desc,
                        episode.number,
                        episode.title
                    ),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            drawRect(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black
                                    ),
                                    startY = 0F,
                                    endY = size.height.times(0.85F)
                                )
                            )
                        },
                    contentAlignment = Alignment.BottomStart
                ) {
                    Text(
                        text = "S${episode.season} E${episode.number}",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(
                            start = 8.dp,
                            bottom = 8.dp,
                        )
                    )
                }
            }

            Box(
                modifier = Modifier.weight(1F)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(
                            top = 10.dp,
                            bottom = 10.dp,
                            end = 10.dp
                        )
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.weight(1F)
                    ) {
                        Text(
                            text = "Episode ${episode.number}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 3.dp)
                        )

                        Text(
                            text = episode.title,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                        )

                        if (episode.runtime != null) {
                            Text(
                                text = "(${episode.runtime})",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Thin
                                ),
                                color = LocalContentColor.current.onMediumEmphasis(),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                            )
                        }
                    }

                    //progress?.let {
                    //    LinearProgressIndicator(
                    //        progress = it,
                    //        color = MaterialTheme.colorScheme.tertiary,
                    //        trackColor = MaterialTheme.colorScheme.tertiary.copy(0.4F),
                    //        modifier = Modifier
                    //            .fillMaxWidth()
                    //            .graphicsLayer {
                    //                shape = RoundedCornerShape(100)
                    //                clip = true
                    //            }
                    //    )
                    //}
                }
            }
        }
    }
}

@Composable
internal fun EpisodeItemPlaceholder() {
    Row(
        modifier = Modifier
            .height(130.dp)
            .padding(horizontal = 10.dp)
            .background(
                Color.Transparent,
                MaterialTheme.shapes.extraSmall
            )
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxHeight()
                .width(180.dp)
                .padding(10.dp)
                .placeholderEffect()
        )

        Box(
            modifier = Modifier
                .weight(1F)
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
            ) {
                Spacer(
                    modifier = Modifier
                        .width(75.dp)
                        .height(12.dp)
                        .padding(bottom = 3.dp)
                        .placeholderEffect()
                )

                Spacer(
                    modifier = Modifier
                        .width(110.dp)
                        .height(14.dp)
                        .padding(bottom = 3.dp)
                        .placeholderEffect()
                )
            }
        }
    }
}