package com.flixclusive.presentation.mobile.common.composables.film

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.flixclusive.R
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.presentation.common.composables.FilmCover
import com.flixclusive.presentation.mobile.main.LABEL_START_PADDING
import com.flixclusive.presentation.mobile.utils.ComposeMobileUtils.colorOnMediumEmphasisMobile
import com.flixclusive.presentation.theme.starColor
import com.flixclusive.presentation.utils.FormatterUtils.formatRating

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilmBottomSheetPreview(
    film: Film,
    sheetState: SheetState,
    isInWatchlist: () -> Boolean,
    isInWatchHistory: () -> Boolean,
    onWatchlistButtonClick: () -> Unit,
    onWatchHistoryButtonClick: () -> Unit,
    onSeeMoreClick: (Film) -> Unit,
    onImageClick: (String?) -> Unit,
    onPlayClick: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null
    ) {

        ConstraintLayout {
            val (background, image, textContent, divider, mainButtons, clickMoreButton) = createRefs()

            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier
                    .constrainAs(background) {
                        top.linkTo(textContent.top)
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            drawRect(surfaceColor)
                        }
                )
            }

            FilmCover.Poster(
                imagePath = film.posterImage,
                imageSize = "w220_and_h330_face",
                onClick = {
                    if (film.posterImage != null) {
                        onImageClick(film.posterImage)
                    }
                },
                modifier = Modifier
                    .width(150.dp)
                    .constrainAs(image) {
                        top.linkTo(parent.top, margin = 10.dp)
                        start.linkTo(parent.start)
                        bottom.linkTo(mainButtons.top, margin = 16.dp)
                    }
                    .padding(horizontal = LABEL_START_PADDING)
            )

            Column(
                modifier = Modifier
                    .constrainAs(textContent) {
                        width = Dimension.fillToConstraints

                        start.linkTo(image.end)
                        end.linkTo(parent.end, margin = LABEL_START_PADDING)
                        bottom.linkTo(mainButtons.top, margin = 12.dp)
                    }
                    .padding(end = LABEL_START_PADDING, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = film.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = stringResource(R.string.rating),
                        modifier = Modifier.scale(0.6F),
                        tint = starColor,
                    )

                    Text(
                        text = "${formatRating(film.rating)} | ${film.dateReleased}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Normal
                    )
                }

                Text(
                    text = film.overview ?: "",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorOnMediumEmphasisMobile(),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2,
                    fontWeight = FontWeight.Light
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .constrainAs(mainButtons) {
                        bottom.linkTo(divider.top, margin = 8.dp)
                    },
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButtonWithLabel(
                    labelId = R.string.play,
                    onClick = onPlayClick
                ) {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = stringResource(R.string.play_button),
                        modifier = Modifier.size(33.dp)
                    )
                }

                val contentDescription = if (isInWatchlist()) {
                    R.string.added_to_watchlist_button
                } else R.string.add_to_watchlist_button

                val icon = if (isInWatchlist()) {
                    R.drawable.added_bookmark
                } else R.drawable.add_bookmark

                val label = if (isInWatchlist()) {
                    R.string.watchlisted
                } else R.string.watchlist

                IconButtonWithLabel(
                    labelId = label,
                    onClick = onWatchlistButtonClick
                ) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = stringResource(contentDescription),
                        modifier = Modifier.size(33.dp)
                    )
                }

                if (isInWatchHistory()) {
                    IconButtonWithLabel(
                        labelId = R.string.remove,
                        onClick = onWatchHistoryButtonClick
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.delete),
                            contentDescription = stringResource(R.string.remove),
                            modifier = Modifier.size(33.dp)
                        )
                    }
                }
            }

            Divider(
                thickness = 1.dp,
                modifier = Modifier
                    .constrainAs(divider) {
                        bottom.linkTo(clickMoreButton.top)
                    }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSeeMoreClick(film) }
                    .constrainAs(clickMoreButton) {
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(parent.bottom)
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.information),
                    contentDescription = stringResource(R.string.more_details),
                    modifier = Modifier
                        .scale(0.7F)
                        .padding(start = LABEL_START_PADDING),
                )

                Text(
                    text = stringResource(R.string.more_details),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .padding(vertical = LABEL_START_PADDING)
                        .weight(1F)
                )

                Icon(
                    painter = painterResource(R.drawable.right_arrow),
                    contentDescription = stringResource(R.string.navigate_to_film),
                    modifier = Modifier
                        .scale(0.7F)
                        .padding(end = LABEL_START_PADDING),
                )
            }
        }

    }
}

@Composable
fun IconButtonWithLabel(
    @StringRes labelId: Int,
    onClick: () -> Unit,
    size: Dp = 65.dp,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(size)
            .background(color = Color.Transparent)
            .clickable(
                onClick = onClick,
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = rememberRipple(
                    bounded = false,
                    radius = size / 2
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            content()

            Text(
                text = stringResource(labelId),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}