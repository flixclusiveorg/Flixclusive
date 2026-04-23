package com.flixclusive.feature.mobile.home.components

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.imageLoader
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.presentation.common.extensions.buildImageRequest
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.common.util.SolidColorPainter
import com.flixclusive.core.presentation.mobile.extensions.isWidthCompact
import com.flixclusive.core.presentation.mobile.extensions.isWidthMedium
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.util.getFeedbackOnLongPress
import com.flixclusive.feature.mobile.home.getBackdropAspectRatio
import com.flixclusive.model.film.Film
import com.flixclusive.core.drawables.R as UiCommonR

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun HomeFilmHeader(
    film: Async<Film>,
    onFilmClick: (Film) -> Unit,
    onFilmLongClick: (Film) -> Unit,
    modifier: Modifier = Modifier,
) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val usePortraitView = windowSizeClass.isWidthCompact || windowSizeClass.isWidthMedium

    Box(
        modifier = modifier.aspectRatio(getBackdropAspectRatio()),
    ) {
        AnimatedContent(
            targetState = film,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.matchParentSize(),
        ) { film ->
            when (film) {
                is Async.Loading -> {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp))
                            .matchParentSize()
                    )

                    ScrimOverlay(usePortraitView = usePortraitView)
                }

                is Async.Success -> FilmContent(
                    film = film.data,
                    onFilmClick = onFilmClick,
                    onFilmLongClick = onFilmLongClick,
                    modifier = Modifier.matchParentSize(),
                )

                is Async.Failure -> { /*No-op*/ }
            }
        }
    }
}

@Composable
private fun FilmContent(
    film: Film,
    onFilmClick: (Film) -> Unit,
    onFilmLongClick: (Film) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val usePortraitView = windowSizeClass.isWidthCompact || windowSizeClass.isWidthMedium

    val feedbackOnLongPress = getFeedbackOnLongPress()
    var showTextInsteadOfLogo by rememberSaveable { mutableStateOf(film.logoImage == null) }
    val logoImage = remember(film) {
        context.buildImageRequest(imagePath = film.logoImage)
    }

    val headerImage = remember(film) {
        context.buildImageRequest(imagePath = film.backdropImage ?: film.posterImage)
    }

    val labelAlignment = if (usePortraitView) {
        Alignment.BottomCenter
    } else {
        Alignment.CenterEnd
    }

    val labelMaxWidth = if (usePortraitView) {
        1f
    } else {
        0.4f
    }

    Box(
        modifier = modifier
    ) {
        AnimatedContent(
            targetState = film,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.matchParentSize(),
        ) { film ->
            AsyncImage(
                model = headerImage,
                imageLoader = LocalContext.current.imageLoader,
                placeholder = SolidColorPainter.from(MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp)),
                error = SolidColorPainter.from(MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp)),
                contentDescription = film.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .combinedClickable(
                        onClick = {
                            onFilmClick(film)
                        },
                        onLongClick = {
                            feedbackOnLongPress()
                            onFilmLongClick(film)
                        },
                    ),
            )

            ScrimOverlay(usePortraitView)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .align(labelAlignment)
                    .fillMaxWidth(labelMaxWidth)
                    .padding(top = 15.dp)
            ) {
                if (showTextInsteadOfLogo) {
                    Text(
                        text = film.title,
                        style = MaterialTheme.typography.headlineMedium.asAdaptiveTextStyle(
                            increaseBy = 5.sp,
                        ),
                        textAlign = TextAlign.Center,
                        softWrap = true,
                        modifier = Modifier.padding(
                            top = 23.dp,
                            start = 5.dp,
                            end = 5.dp,
                        ),
                    )
                } else {
                    AsyncImage(
                        model = logoImage,
                        imageLoader = LocalContext.current.imageLoader,
                        error = painterResource(UiCommonR.drawable.sample_movie_subtitle_preview),
                        contentDescription = film.title,
                        onError = { showTextInsteadOfLogo = true },
                        modifier = Modifier
                            .height(getAdaptiveDp(96.dp, 20.dp))
                            .fillMaxWidth()
                            .padding(horizontal = 25.dp),
                    )
                }

                FlowRow(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                ) {
                    film.genres.forEachIndexed { index, genre ->
                        Text(
                            text = genre.name,
                            style = MaterialTheme.typography.labelMedium.asAdaptiveTextStyle(
                                increaseBy = 5.sp,
                            ),
                            color = LocalContentColor.current.copy(0.6f),
                        )

                        if (index < film.genres.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .size(getAdaptiveDp(2.dp, 2.dp))
                                    .background(
                                        color = LocalContentColor.current.copy(0.6f),
                                        shape = CircleShape,
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.ScrimOverlay(usePortraitView: Boolean) {
    val backgroundColor = MaterialTheme.colorScheme.background

    Box(
        modifier = Modifier
            .matchParentSize()
            .statusBarsPadding()
            .drawWithCache {
                onDrawWithContent {
                    drawContent()
                    drawRect(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                backgroundColor,
                            ),
                            endY = size.height.times(0.9F),
                        ),
                    )

                    if (!usePortraitView) {
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    backgroundColor,
                                ),
                                start = Offset(
                                    size.width.times(0.2f),
                                    size.height.times(0.5f),
                                ),
                                end = Offset(
                                    size.width.times(0.9f),
                                    0f,
                                ),
                            ),
                        )
                    }
                }
            },
    )
}

@Preview
@Composable
private fun HomeFilmHeaderBasePreview() {
    var film by remember {
        mutableStateOf<Async<Film>>(
            Async.Success(
                DummyDataForPreview.getFilm(
                    genres = listOf(
                        "Action",
                        "Adventure",
                        "Fantasy",
                    ),
                )
            )
        )
    }

    FlixclusiveTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            Column {
                HomeFilmHeader(
                    film = film,
                    onFilmClick = {},
                    onFilmLongClick = {},
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun DisplayHeaderCompactLandscapePreview() {
    HomeFilmHeaderBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun DisplayHeaderMediumPortraitPreview() {
    HomeFilmHeaderBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun DisplayHeaderMediumLandscapePreview() {
    HomeFilmHeaderBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun DisplayHeaderExtendedPortraitPreview() {
    HomeFilmHeaderBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun DisplayHeaderExtendedLandscapePreview() {
    HomeFilmHeaderBasePreview()
}
