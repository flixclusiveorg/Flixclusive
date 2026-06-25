package com.flixclusive.core.presentation.mobile.components.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.size.Size
import com.flixclusive.core.drawables.R
import com.flixclusive.core.presentation.common.components.MediaCover
import com.flixclusive.core.presentation.common.util.DummyDataForPreview.getMedia
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.theme.MobileColors.surfaceColorAtElevation
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.model.media.MediaMetadata

val MediaCardDefaultPosterSize = Size(
    width = 100,
    height = 300,
)

/**
 * A card component that displays a media's poster and optionally its title
 * if user prefers to see it.
 *
 * @param media The media to be displayed.
 * @param onClick A lambda function to be invoked when the card is clicked.
 * @param onLongClick A lambda function to be invoked when the card is long-clicked.
 * @param modifier An optional [Modifier] for this component.
 * @param isShowingTitle A boolean indicating whether to show the media's title below the poster
 * */
@Composable
fun MediaCard(
    media: MediaMetadata,
    onClick: (MediaMetadata) -> Unit,
    onLongClick: (MediaMetadata) -> Unit,
    modifier: Modifier = Modifier,
    isShowingTitle: Boolean = false,
) {
    var showPlaceholder by rememberSaveable { mutableStateOf(true) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(3.dp).then(modifier),
    ) {
        Box(
            contentAlignment = Alignment.Center,
        ) {
            MediaCover.Poster(
                imagePath = media.posterImage,
                imageSize = MediaCardDefaultPosterSize,
                title = media.title,
                onSuccess = { showPlaceholder = false },
                onClick = { onClick(media) },
                onLongClick = { onLongClick(media) },
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(3),
                        shape = MaterialTheme.shapes.small,
                    ),
            )

            if (showPlaceholder) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.matchParentSize(),
                ) {
                    Box(
                        contentAlignment = Alignment.BottomCenter,
                        modifier = Modifier.weight(0.4f),
                    ) {
                        AdaptiveIcon(
                            painter = painterResource(id = R.drawable.movie_icon),
                            contentDescription = media.title,
                            tint = LocalContentColor.current.copy(0.6f),
                            dp = 40.dp,
                            increaseBy = 10.dp,
                        )
                    }

                    Box(
                        contentAlignment = Alignment.TopCenter,
                        modifier = Modifier.weight(0.6F),
                    ) {
                        Text(
                            text = media.title,
                            style = MaterialTheme.typography.labelLarge.asAdaptiveTextStyle(size = 12.sp),
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis,
                            color = LocalContentColor.current.copy(0.6f),
                            modifier = Modifier
                                .padding(8.dp),
                        )
                    }
                }
            }
        }

        if (isShowingTitle) {
            Text(
                text = media.title,
                style = MaterialTheme.typography.labelLarge.asAdaptiveTextStyle(size = 12.sp),
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                color = LocalContentColor.current.copy(alpha = 0.8F),
                maxLines = 1,
                modifier = Modifier.padding(vertical = 5.dp),
            )
        }
    }
}

@Preview
@Composable
private fun MediaCardPreview() {
    FlixclusiveTheme {
        Surface {
            LazyRow {
                items(20) {
                    MediaCard(
                        media = getMedia(),
                        onClick = {},
                        onLongClick = {},
                    )
                }
            }
        }
    }
}
