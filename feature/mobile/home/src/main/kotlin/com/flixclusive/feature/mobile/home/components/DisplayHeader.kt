package com.flixclusive.feature.mobile.home.components

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.imageLoader
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.presentation.common.components.FilmCover
import com.flixclusive.core.presentation.common.extensions.buildTMDBImageUrl
import com.flixclusive.core.presentation.common.theme.Colors
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.common.util.FilmFormatterUtil.formatAsRating
import com.flixclusive.core.presentation.common.util.SolidColorPainter
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.Companion.getAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.TypographySize
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.RetryButton
import com.flixclusive.core.presentation.mobile.extensions.isCompact
import com.flixclusive.core.presentation.mobile.extensions.isMedium
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.core.presentation.mobile.util.getFeedbackOnLongPress
import com.flixclusive.feature.mobile.home.R
import com.flixclusive.model.film.Film
import kotlinx.coroutines.delay
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

private const val LANDSCAPE_RATIO = 16f / 6f

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun DisplayHeader(
    film: Film?,
    error: UiText?,
    onFilmClick: (Film) -> Unit,
    onFilmLongClick: (Film) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val feedbackOnLongPress = getFeedbackOnLongPress()
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val usePortraitView = windowSizeClass.windowWidthSizeClass.isCompact
        || windowSizeClass.windowWidthSizeClass.isMedium

    var showTextInsteadOfLogo by rememberSaveable { mutableStateOf(false) }
    val logoImage = remember(film) {
        context.buildTMDBImageUrl(
            imagePath = film?.logoImage,
            imageSize = "w500",
        )
    }
    val headerImage = remember(film) {
        val size = when {
            usePortraitView -> "w600_and_h900_multi_faces"
            else -> "w1920_and_h600_multi_faces"
        }

        context.buildTMDBImageUrl(
            imagePath = film?.backdropImage,
            imageSize = size,
        )
    }

    val itemDescription = remember(film) {
        val rating = film?.rating?.formatAsRating()?.asString(context)

        if (rating != null) {
            "$rating | ${film.parsedReleaseDate}"
        } else {
            film?.parsedReleaseDate ?: context.getString(LocaleR.string.no_release_date)
        }
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
            .aspectRatio(
                when {
                    windowSizeClass.windowWidthSizeClass.isMedium -> 2.8f / 3f
                    usePortraitView -> FilmCover.Poster.ratio
                    else -> LANDSCAPE_RATIO
                },
            ),
    ) {
        AnimatedContent(
            targetState = error != null,
            modifier = Modifier.fillMaxSize(),
        ) { hasErrors ->
            when (hasErrors) {
                true -> {
                    val errorMessage = error?.asString()
                        ?: stringResource(R.string.failed_to_get_header_item)

                    ScrimOverlay(usePortraitView)

                    RetryButton(
                        modifier = Modifier.matchParentSize(),
                        shouldShowError = true,
                        error = errorMessage,
                        onRetry = onRetry,
                    )
                }

                false -> {
                    AsyncImage(
                        model = headerImage,
                        imageLoader = LocalContext.current.imageLoader,
                        placeholder = SolidColorPainter(MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp)),
                        error = SolidColorPainter(MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp)),
                        contentDescription = film?.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .combinedClickable(
                                onClick = {
                                    if (film != null) {
                                        onFilmClick(film)
                                    }
                                },
                                onLongClick = {
                                    if (film != null) {
                                        feedbackOnLongPress()
                                        onFilmLongClick(film)
                                    }
                                },
                            ),
                    )

                    ScrimOverlay(usePortraitView)

                    AnimatedContent(
                        label = "DisplayHeaderLabelContent",
                        targetState = film,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        modifier = Modifier
                            .align(labelAlignment)
                            .fillMaxWidth(labelMaxWidth)
                            .padding(top = 15.dp),
                    ) DisplayHeaderLabelContent@ { state ->
                        if (state == null) {
                            return@DisplayHeaderLabelContent
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                        ) {
                            if (showTextInsteadOfLogo) {
                                Text(
                                    text = state.title,
                                    style = getAdaptiveTextStyle(
                                        style = AdaptiveTextStyle.Emphasized(size = TypographySize.Headline),
                                        size = 24.sp,
                                        increaseBy = 5.sp
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
                                    contentDescription = state.title,
                                    onError = { showTextInsteadOfLogo = true },
                                    modifier = Modifier
                                        .height(getAdaptiveDp(96.dp, 20.dp))
                                        .fillMaxWidth()
                                        .padding(horizontal = 25.dp),
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 15.dp),
                            ) {
                                AdaptiveIcon(
                                    imageVector = Icons.Rounded.Star,
                                    contentDescription = stringResource(LocaleR.string.rating_icon),
                                    modifier = Modifier.scale(0.6F),
                                    tint = Colors.starColor,
                                    increaseBy = 10.dp
                                )

                                Text(
                                    text = itemDescription,
                                    style = getAdaptiveTextStyle(size = 12.sp),
                                    fontWeight = FontWeight.Medium,
                                    color = LocalContentColor.current.copy(0.6f),
                                )
                            }

                            FlowRow(
                                modifier = Modifier.padding(top = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            ) {
                                state.genres.forEachIndexed { index, genre ->
                                    Text(
                                        text = genre.name,
                                        style = getAdaptiveTextStyle(size = 12.sp),
                                        fontWeight = FontWeight.Medium,
                                        color = LocalContentColor.current.copy(0.6f),
                                    )

                                    if (index < state.genres.lastIndex) {
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
                                    backgroundColor
                                ),
                                start = Offset(
                                    size.width.times(0.2f),
                                    size.height.times(0.5f)
                                ),
                                end = Offset(
                                    size.width.times(0.9f),
                                    0f
                                )
                            )
                        )
                    }
                }
            },
    )
}

@Preview
@Composable
private fun DisplayHeaderBasePreview() {
    var film by remember { mutableStateOf<Film?>(null) }

    LaunchedEffect(Unit) {
        delay(2000)
        film = DummyDataForPreview.getDummyFilm(
            genres = listOf(
                "Action",
                "Adventure",
                "Fantasy",
            )
        )
    }

    FlixclusiveTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            Column {
                DisplayHeader(
                    film = film,
                    onFilmClick = {},
                    onFilmLongClick = {},
                    error = null,
                    onRetry = {},
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun DisplayHeaderCompactLandscapePreview() {
    DisplayHeaderBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun DisplayHeaderMediumPortraitPreview() {
    DisplayHeaderBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun DisplayHeaderMediumLandscapePreview() {
    DisplayHeaderBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun DisplayHeaderExtendedPortraitPreview() {
    DisplayHeaderBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun DisplayHeaderExtendedLandscapePreview() {
    DisplayHeaderBasePreview()
}
