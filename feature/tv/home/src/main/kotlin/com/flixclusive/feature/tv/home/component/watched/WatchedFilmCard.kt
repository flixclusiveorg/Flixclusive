package com.flixclusive.feature.tv.home.component.watched

import android.graphics.drawable.Drawable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import androidx.tv.material3.Border
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.CardLayoutDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Glow
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.StandardCardLayout
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.imageLoader
import com.flixclusive.core.locale.UiText
import com.flixclusive.core.ui.common.FilmCover
import com.flixclusive.core.ui.common.util.buildImageUrl
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.tv.component.CustomLinearProgressIndicator
import com.flixclusive.core.ui.tv.component.DotSeparatedText
import com.flixclusive.core.ui.tv.component.FilmCardShape
import com.flixclusive.core.ui.tv.component.FilmPadding
import com.flixclusive.feature.tv.home.component.util.useLocalImmersiveBackgroundColor
import com.flixclusive.model.database.WatchHistoryItem
import com.flixclusive.model.database.util.getNextEpisodeToWatch
import com.flixclusive.model.film.util.FilmType
import com.flixclusive.core.ui.common.util.formatMinutes
import com.flixclusive.core.locale.R as LocaleR

internal val WatchedFilmCardHeight = 250.dp
private val WatchedFilmCardWidth = 400.dp

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun WatchedFilmCard(
    modifier: Modifier = Modifier,
    watchHistoryItem: WatchHistoryItem,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val film = watchHistoryItem.film
    val immersiveBackgroundColor = useLocalImmersiveBackgroundColor()

    var isFocused by remember { mutableStateOf(false) }
    var drawable: Drawable? by remember { mutableStateOf(null) }

    LaunchedEffect(isFocused, drawable) {
        if (isFocused && drawable != null) {
            Palette
                .from(drawable!!.toBitmap())
                .generate()
                .vibrantSwatch
                ?.rgb?.let {
                    immersiveBackgroundColor.value = Color(it)
                }
        }
    }

    val borderDp by infiniteTransition.animateValue(
        initialValue = 0.dp,
        targetValue = 3.dp,
        typeConverter = Dp.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )

    StandardCardLayout(
        modifier = modifier
            .padding(FilmPadding.getPaddingValues())
            .onFocusChanged { isFocused = it.isFocused },
        imageCard = {
            CardLayoutDefaults.ImageCard(
                onClick = onClick,
                interactionSource = it,
                shape = CardDefaults.shape(FilmCardShape),
                glow = CardDefaults.glow(
                    focusedGlow = Glow(
                        elevationColor = MaterialTheme.colorScheme.primary.onMediumEmphasis(),
                        elevation = 15.dp
                    ),
                    pressedGlow = Glow(
                        elevationColor = MaterialTheme.colorScheme.primary.onMediumEmphasis(),
                        elevation = 40.dp
                    ),
                ),
                border = CardDefaults.border(
                    focusedBorder = Border(
                        border = BorderStroke(
                            width = borderDp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = FilmCardShape
                    )
                ),
                scale = CardDefaults.scale(focusedScale = 1F),
            ) {
                Box(
                    modifier = Modifier
                        .size(
                            height = WatchedFilmCardHeight,
                            width = WatchedFilmCardWidth,
                        )
                ) {
                    CardImage(
                        backdropImage = film.backdropImage,
                        onImageLoad = { image -> drawable = image },
                        onClick = onClick
                    )

                    CardProgress(
                        isTvShow = film.filmType == FilmType.TV_SHOW,
                        watchHistoryItem = watchHistoryItem,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                    )
                }
            }
        },
        title = {
            CardOverview(item = watchHistoryItem)
        }
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CardImage(
    backdropImage: String?,
    onImageLoad: (Drawable) -> Unit,
    onClick: () -> Unit,
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .drawWithCache {
                onDrawWithContent {
                    drawContent()
                    drawRect(
                        Brush.verticalGradient(
                            0.6F to Color.Transparent,
                            1F to Color.Black
                        )
                    )
                }
            }
    ) {
        val painter = remember(backdropImage) {
            context.buildImageUrl(
                imagePath = backdropImage,
                imageSize = "w780"
            )?.newBuilder(context)
                ?.allowHardware(false)
                ?.build()
        }

        AsyncImage(
            model = painter,
            imageLoader = LocalContext.current.imageLoader,
            contentScale = ContentScale.FillBounds,
            contentDescription = stringResource(id = LocaleR.string.film_item_content_description),
            onSuccess = { onImageLoad(it.result.drawable) },
            modifier = Modifier
                .aspectRatio(FilmCover.Backdrop.ratio)
                .clip(MaterialTheme.shapes.extraSmall)
                .clickable { onClick() }
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CardProgress(
    modifier: Modifier = Modifier,
    isTvShow: Boolean,
    watchHistoryItem: WatchHistoryItem,
) {
    val lastWatchedEpisode = watchHistoryItem.episodesWatched.last()
    var progress by remember(watchHistoryItem) {
        val percentage = if(lastWatchedEpisode.durationTime == 0L) {
            0F
        } else {
            lastWatchedEpisode.watchTime.toFloat() / lastWatchedEpisode.durationTime.toFloat()
        }

        mutableFloatStateOf(percentage)
    }

    val itemLabel = remember(watchHistoryItem) {
        if(isTvShow) {
            val nextEpisodeWatched = getNextEpisodeToWatch(watchHistoryItem)
            val season = nextEpisodeWatched.first
            val episode = nextEpisodeWatched.second

            val lastEpisodeIsNotSameWithNextEpisodeToWatch = lastWatchedEpisode.episodeNumber != episode

            if(lastEpisodeIsNotSameWithNextEpisodeToWatch)
                progress = 0F

            UiText.StringValue("S${season} E${episode}")
        } else {
            val watchTime = watchHistoryItem.episodesWatched.last().watchTime
            val watchTimeInSeconds = (watchTime / 1000).toInt()
            val watchTimeInMinutes = watchTimeInSeconds / 60

            formatMinutes(totalMinutes = watchTimeInMinutes)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = itemLabel.asString(),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Black,
                fontSize = 13.sp
            )
        )

        CustomLinearProgressIndicator(
            progress = progress,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CardOverview(
    modifier: Modifier = Modifier,
    item: WatchHistoryItem,
) {
    val context = LocalContext.current

    val filmInfo = remember {
        val infoList = mutableListOf<String>()
        infoList.apply {
            with(item) {
                if (film.filmType ==  FilmType.MOVIE) {
                    add(formatMinutes(episodesWatched.firstOrNull()?.durationTime?.toInt()?.div(1000)?.div(60)).asString(context))
                } else {
                    val averageRuntime = (episodesWatched.map { it.durationTime }.average().toInt() / 1000) / 60
                    if (averageRuntime > 0) {
                        add(formatMinutes(averageRuntime).asString(context))
                    }

                    if(seasons != null) {
                        var seasonsRuntime = UiText.StringResource(LocaleR.string.season_runtime_formatter, seasons!!).asString(context)

                        if(seasons!! > 1)
                            seasonsRuntime += 's'

                        add(seasonsRuntime)
                    }


                    val totalEpisodes = episodes.values.sum()
                    if (totalEpisodes > 0) {
                        var episodesRuntime = UiText.StringResource(LocaleR.string.episode_runtime_formatter, totalEpisodes).asString(context)

                        if(totalEpisodes > 1)
                            episodesRuntime += 's'

                        add(episodesRuntime)
                    }
                }

                film.parsedReleaseDate?.let(::add)
            }
        }
            .toList()
            .filterNot { it.isEmpty() }
    }

    Column(
        modifier = modifier
            .width(WatchedFilmCardWidth)
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = item.film.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Thin,
                fontSize = 22.sp
            ),
            color = LocalContentColor.current.onMediumEmphasis(0.5F),
            modifier = Modifier
                .padding(bottom = 5.dp)
        )

        DotSeparatedText(
            texts = filmInfo,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Medium,
            ),
            color = LocalContentColor.current.onMediumEmphasis(0.8F),
            modifier = Modifier
                .fillMaxWidth(0.85F)
        )

        item.film.overview?.let {
            Text(
                text = it,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Normal,
                ),
                color = LocalContentColor.current.onMediumEmphasis(0.8F)
            )
        }
    }
}