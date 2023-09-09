package com.flixclusive.presentation.tv.screens.film

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.flixclusive.domain.common.Resource
import com.flixclusive.domain.model.tmdb.Season
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.domain.model.tmdb.TvShow
import com.flixclusive.presentation.tv.utils.ComposeTvUtils.NonFocusableSpacer
import com.flixclusive.presentation.tv.utils.ComposeTvUtils.colorOnMediumEmphasisTv
import com.flixclusive.presentation.tv.utils.ComposeTvUtils.hasPressedLeft
import com.flixclusive.presentation.tv.utils.ModifierTvUtils.LabelStartPadding
import com.flixclusive.presentation.tv.utils.ModifierTvUtils.createInitialFocusRestorerModifiers
import com.flixclusive.presentation.tv.utils.ModifierTvUtils.focusOnInitialVisibility
import com.flixclusive.presentation.tv.utils.ModifierTvUtils.ifElse
import com.flixclusive.presentation.utils.ImageRequestCreator.buildImageUrl
import com.flixclusive.presentation.utils.ModifierUtils.fadingEdge
import com.flixclusive.presentation.utils.ModifierUtils.placeholderEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FilmTvEpisodesPanel(
    isVisible: Boolean,
    film: TvShow,
    currentSelectedSeasonNumber: Int,
    currentSelectedSeason: Resource<Season>,
    onSeasonChange: (Int) -> Unit,
    onEpisodeClick: (TMDBEpisode) -> Unit,
    onHidePanel: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val topFade = Brush.verticalGradient(
        0F to Color.Transparent,
        0.16F to Color.Red
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val isInitiallyVisible = remember { mutableStateOf(false) }
        val seasonsFocusRequesterModifier = createInitialFocusRestorerModifiers()
        var seasonsTabHasFocus by remember { mutableStateOf(false) }

        var seasonName by remember { mutableStateOf("") }

        LaunchedEffect(currentSelectedSeason) {
            when(currentSelectedSeason) {
                is Resource.Success -> {
                    seasonName = currentSelectedSeason.data?.name ?: return@LaunchedEffect
                }
                else -> return@LaunchedEffect
            }
        }

        BackHandler {
            onHidePanel()
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(colorOnMediumEmphasisTv(MaterialTheme.colorScheme.surface))
                .onPreviewKeyEvent {
                    if (hasPressedLeft(it) && seasonsTabHasFocus) {
                        onHidePanel()
                        return@onPreviewKeyEvent true
                    }
                    false
                },
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TvLazyColumn(
                pivotOffsets = PivotOffsets(0.16F),
                contentPadding = PaddingValues(top = LabelStartPadding.start),
                modifier = seasonsFocusRequesterModifier.parentModifier
                    .padding(start = 100.dp, top = 100.dp)
                    .fadingEdge(topFade)
                    .onFocusChanged {
                        scope.launch {
                            seasonsTabHasFocus = if (it.hasFocus) {
                                delay(500)
                                true
                            } else false
                        }
                    }
            ) {
                item {
                    NonFocusableSpacer(height = 40.dp)
                }

                itemsIndexed(
                    film.seasons
                ) { i, season ->
                    SeasonItem(
                        seasonNumber = season.seasonNumber,
                        currentSelectedSeasonNumber = currentSelectedSeasonNumber,
                        onSeasonChange = {
                            onSeasonChange(season.seasonNumber)
                        },
                        modifier = Modifier
                            .ifElse(
                                condition = i == 0,
                                ifTrueModifier = seasonsFocusRequesterModifier.childModifier
                            )
                            .ifElse(
                                condition = i == 0 && !isInitiallyVisible.value,
                                ifTrueModifier = Modifier.focusOnInitialVisibility(isVisible = isInitiallyVisible)
                            )
                    )
                }

                items(10) {
                    NonFocusableSpacer(height = 40.dp)
                }
            }

            TvLazyColumn(
                pivotOffsets = PivotOffsets(0.13F),
                modifier = Modifier
                    .weight(1F)
                    .fillMaxHeight()
            ) {
                stickyHeader {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = colorOnMediumEmphasisTv(
                                    color = MaterialTheme.colorScheme.surface,
                                    emphasis = 0.2F
                                )
                            ),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = seasonName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            ),
                            modifier = Modifier
                                .padding(25.dp)
                        )
                    }
                }

                if(currentSelectedSeason is Resource.Success) {
                    items(
                        items = currentSelectedSeason.data!!.episodes,
                        key = { it.episodeId }
                    ) { episode ->
                        EpisodeItem(
                            episode = episode,
                            onEpisodeClick = { onEpisodeClick(episode) }
                        )
                    }
                }

                if(currentSelectedSeason is Resource.Loading) {
                    items(5) {
                        EpisodeItemPlaceholder()
                    }
                }
            }
        }
    }
}

@Composable
private fun SeasonItem(
    modifier: Modifier = Modifier,
    seasonNumber: Int,
    currentSelectedSeasonNumber: Int,
    onSeasonChange: () -> Unit
) {
    val isSelected = remember(currentSelectedSeasonNumber) { currentSelectedSeasonNumber == seasonNumber }
    var isFocused by remember { mutableStateOf(false) }


    val style = if(isSelected) {
        MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Bold
        )
    } else {
        MaterialTheme.typography.bodyMedium
    }
    val focusedBorder = Border(
        border = BorderStroke(2.dp, Color.White),
        shape = RectangleShape
    )

    Surface(
        modifier = modifier
            .width(200.dp)
            .onFocusChanged {
                isFocused = it.isFocused

                if (isFocused) {
                    onSeasonChange()
                }
            },
        border = ClickableSurfaceDefaults.border(
            border = if(isSelected) focusedBorder else Border.None,
            focusedBorder = focusedBorder
        ),
        shape = ClickableSurfaceDefaults.shape(RectangleShape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1F),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = if(isSelected) Color.White else colorOnMediumEmphasisTv(emphasis = 0.8F),
            focusedContainerColor = Color.Transparent,
            focusedContentColor = Color.White
        ),
        onClick = {}
    ) {
        Text(
            text = "Season $seasonNumber",
            style = style,
            modifier = Modifier
                .padding(16.dp)
        )
    }
}


@Composable
private fun EpisodeItem(
    modifier: Modifier = Modifier,
    episode: TMDBEpisode,
    onEpisodeClick: () -> Unit
) {
    val context = LocalContext.current

    val shape = MaterialTheme.shapes.extraSmall
    var isFocused by remember { mutableStateOf(false) }
    val borderFocused = remember(isFocused) {
        if(isFocused) {
            BorderStroke(width = 2.dp, color = Color.White)
        } else BorderStroke(width = 0.dp, color = Color.Transparent)
    }

    Surface(
        onClick = onEpisodeClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = colorOnMediumEmphasisTv(),
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
                        imageSize = "w533_and_h300_bestv2"
                    ),
                    contentDescription = "An image of episode ${episode.episode}: ${episode.title}",
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
                        text = "S${episode.season} E${episode.episode}",
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
                            text = "Episode ${episode.episode}",
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

                        if(episode.runtime != null) {
                            Text(
                                text = "(${episode.runtime})",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Thin
                                ),
                                color = colorOnMediumEmphasisTv(),
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
private fun EpisodeItemPlaceholder() {
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