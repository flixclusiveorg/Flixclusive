package com.flixclusive.mobile.component

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
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
import com.flixclusive.core.theme.starColor
import com.flixclusive.core.ui.common.FilmCover
import com.flixclusive.core.ui.common.util.formatRating
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.model.film.Film
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.ui.mobile.R as UiMobileR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilmPreviewBottomSheet(
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
                imageSize = "w300",
                showPlaceholder = false,
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
                    .padding(horizontal = 15.dp)
            )

            Column(
                modifier = Modifier
                    .constrainAs(textContent) {
                        width = Dimension.fillToConstraints

                        start.linkTo(image.end)
                        end.linkTo(parent.end, margin = 15.dp)
                        bottom.linkTo(mainButtons.top, margin = 12.dp)
                    }
                    .padding(end = 15.dp, top = 8.dp),
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
                        contentDescription = stringResource(LocaleR.string.rating_icon),
                        modifier = Modifier.scale(0.6F),
                        tint = starColor,
                    )

                    Text(
                        text = "${formatRating(film.rating).asString()} | ${film.parsedReleaseDate}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Normal
                    )
                }

                Text(
                    text = film.overview ?: "",
                    style = MaterialTheme.typography.labelMedium,
                    color = LocalContentColor.current.onMediumEmphasis(),
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
                    labelId = LocaleR.string.play,
                    onClick = onPlayClick
                ) {
                    Icon(
                        painter = painterResource(UiCommonR.drawable.play),
                        contentDescription = stringResource(LocaleR.string.play_button),
                        modifier = Modifier.size(33.dp)
                    )
                }

                val contentDescription = if (isInWatchlist()) {
                    LocaleR.string.added_to_watchlist_button
                } else LocaleR.string.add_to_watchlist_button

                val icon = if (isInWatchlist()) {
                    R.drawable.added_bookmark
                } else R.drawable.add_bookmark

                val label = if (isInWatchlist()) {
                    LocaleR.string.watchlisted
                } else LocaleR.string.watchlist

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
                        labelId = LocaleR.string.remove,
                        onClick = onWatchHistoryButtonClick
                    ) {
                        Icon(
                            painter = painterResource(UiCommonR.drawable.delete),
                            contentDescription = stringResource(LocaleR.string.remove),
                            modifier = Modifier.size(33.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier
                    .constrainAs(divider) {
                        bottom.linkTo(clickMoreButton.top)
                    },
                thickness = 1.dp
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
                    contentDescription = stringResource(LocaleR.string.more_details),
                    modifier = Modifier
                        .scale(0.7F)
                        .padding(start = 15.dp),
                )

                Text(
                    text = stringResource(LocaleR.string.more_details),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .padding(vertical = 15.dp)
                        .weight(1F)
                )

                Icon(
                    painter = painterResource(UiMobileR.drawable.right_arrow),
                    contentDescription = stringResource(LocaleR.string.navigate_to_film),
                    modifier = Modifier
                        .scale(0.7F)
                        .padding(end = 15.dp),
                )
            }
        }

    }
}

@Composable
internal fun IconButtonWithLabel(
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
                indication = ripple(
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