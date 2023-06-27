package com.flixclusive.presentation.film.bottom_sheet_content

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import coil.compose.AsyncImage
import com.flixclusive.R
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.presentation.common.Formatter.formatRating
import com.flixclusive.presentation.common.IconResource
import com.flixclusive.presentation.common.ImageRequestCreator.buildImageUrl
import com.flixclusive.presentation.common.UiText
import com.flixclusive.presentation.main.LABEL_START_PADDING
import com.flixclusive.ui.theme.colorOnMediumEmphasis
import com.flixclusive.ui.theme.starColor

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
    val context = LocalContext.current

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

           AsyncImage(
               model = context.buildImageUrl(
                   imagePath = film.posterImage, imageSize = "w342"
               ),
               placeholder = IconResource.fromDrawableResource(R.drawable.movie_placeholder).asPainterResource(),
               contentDescription = String.format(UiText.StringResource(R.string.poster_content_description).asString(), film.title),
               contentScale = ContentScale.Crop,
               modifier = Modifier
                   .constrainAs(image) {
                       top.linkTo(parent.top, margin = 10.dp)
                       start.linkTo(parent.start)
                       bottom.linkTo(mainButtons.top, margin = 16.dp)
                   }
                   .height(180.dp)
                   .width(140.dp)
                   .padding(horizontal = LABEL_START_PADDING)
                   .graphicsLayer {
                       shape = RoundedCornerShape(10)
                       clip = true
                   }
                   .clickable {
                       onImageClick(film.posterImage)
                   }
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
                       painter = IconResource.fromImageVector(Icons.Rounded.Star).asPainterResource(),
                       contentDescription = UiText.StringResource(R.string.rating).asString(),
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
                   color = colorOnMediumEmphasis(),
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
                   label = UiText.StringResource(R.string.play),
                   onClick = onPlayClick
               ) {
                   Icon(
                       painter = IconResource.fromDrawableResource(R.drawable.play).asPainterResource(),
                       contentDescription = UiText.StringResource(R.string.play_button).asString(),
                       modifier = Modifier.size(33.dp)
                   )
               }

               val contentDescription = if(isInWatchlist()) {
                   UiText.StringResource(R.string.added_to_watchlist_button)
               } else UiText.StringResource(R.string.add_to_watchlist_button)

               val icon = if(isInWatchlist()) {
                   IconResource.fromDrawableResource(R.drawable.added_bookmark)
               } else IconResource.fromDrawableResource(R.drawable.add_bookmark)

               val label = if(isInWatchlist()) {
                   UiText.StringResource(R.string.watchlisted)
               } else UiText.StringResource(R.string.watchlist)

               IconButtonWithLabel(
                   label = label,
                   onClick = onWatchlistButtonClick
               ) {
                   Icon(
                       painter = icon.asPainterResource(),
                       contentDescription = contentDescription.asString(),
                       modifier = Modifier.size(33.dp)
                   )
               }

               if(isInWatchHistory()) {
                   IconButtonWithLabel(
                       label = UiText.StringResource(R.string.remove),
                       onClick = onWatchHistoryButtonClick
                   ) {
                       Box(modifier = Modifier.size(33.dp)) {
                           Icon(
                               painter = IconResource.fromDrawableResource(R.drawable.delete)
                                   .asPainterResource(),
                               contentDescription = UiText.StringResource(R.string.remove)
                                   .asString(),
                               modifier = Modifier.size(25.dp)
                                   .align(Alignment.Center)
                           )
                       }
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
                   painter = IconResource.fromDrawableResource(R.drawable.information).asPainterResource(),
                   contentDescription = UiText.StringResource(R.string.more_details).asString(),
                   modifier = Modifier
                       .scale(0.7F)
                       .padding(start = LABEL_START_PADDING),
               )

               Text(
                   text = UiText.StringResource(R.string.more_details).asString(),
                   style = MaterialTheme.typography.titleMedium,
                   modifier = Modifier
                       .padding(vertical = LABEL_START_PADDING)
                       .weight(1F)
               )

               Icon(
                   painter = IconResource.fromDrawableResource(R.drawable.right_arrow).asPainterResource(),
                   contentDescription = UiText.StringResource(R.string.navigate_to_film).asString(),
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
    label: UiText,
    onClick: () -> Unit,
    size: Dp = 65.dp,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
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
                text = label.asString(),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}