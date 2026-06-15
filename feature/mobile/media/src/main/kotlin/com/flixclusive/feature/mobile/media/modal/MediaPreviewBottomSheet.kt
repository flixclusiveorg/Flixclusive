package com.flixclusive.feature.mobile.media.modal

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.flixclusive.core.presentation.common.components.MediaCover
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.common.util.MediaDetailsFormatterUtil.formatAsRating
import com.flixclusive.core.presentation.mobile.components.media.MediaCardDefaultPosterSize
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.feature.mobile.media.R
import com.flixclusive.feature.mobile.media.navigator.NavigatorMediaPreviewBottomSheet
import com.flixclusive.model.media.MediaMetadata
import java.util.Calendar
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.presentation.mobile.R as UiMobileR
import com.flixclusive.core.strings.R as LocaleR

data class MediaPreviewNavArgs(
    val media: MediaMetadata,
)

@Composable
fun MediaPreviewBottomSheet(
    args: MediaPreviewNavArgs,
    navigator: NavigatorMediaPreviewBottomSheet,
) {
    val media = remember { args.media }

    MediaPreviewBottomSheetContent(
        media = media,
        showMediaImageDialog = navigator::showMediaImageDialog,
        showLinkLoaderSheet = navigator::showLinkLoaderSheet,
        navigateToMediaScreen = navigator::navigateToMediaScreen,
    )
}

@Composable
private fun MediaPreviewBottomSheetContent(
    media: MediaMetadata,
    showMediaImageDialog: (String) -> Unit,
    showLinkLoaderSheet: (MediaMetadata) -> Unit,
    navigateToMediaScreen: (MediaMetadata, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColor = MaterialTheme.colorScheme.surface

    ConstraintLayout(
        modifier = modifier
            .background(Color.Transparent)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        val (background, image, textContent, divider, mainButtons, clickMoreButton) = createRefs()

        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier
                .constrainAs(background) {
                    top.linkTo(textContent.top)
                },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(surfaceColor)
                    },
            )
        }

        MediaCover.Poster(
            imagePath = media.posterImage,
            imageSize = MediaCardDefaultPosterSize,
            title = media.title,
            onClick = {
                val poster = media.posterImage ?: return@Poster
                showMediaImageDialog(poster)
            },
            modifier = Modifier
                .width(150.dp)
                .constrainAs(image) {
                    top.linkTo(parent.top, margin = 10.dp)
                    start.linkTo(parent.start)
                    bottom.linkTo(mainButtons.top, margin = 16.dp)
                }.padding(horizontal = 15.dp),
        )

        Column(
            modifier = Modifier
                .constrainAs(textContent) {
                    width = Dimension.fillToConstraints

                    start.linkTo(image.end)
                    end.linkTo(parent.end, margin = 15.dp)
                    bottom.linkTo(mainButtons.top, margin = 12.dp)
                }.padding(end = 15.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = media.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            if (media.rating != null || media.releaseDate != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    media.rating?.let {
                        Text(
                            text = it.formatAsRating().asString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onTertiary,
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.tertiary.copy(0.6f),
                                    shape = MaterialTheme.shapes.extraSmall,
                                ).padding(horizontal = 3.dp, vertical = 1.dp),
                        )
                    }

                    media.releaseDate?.let {
                        val year = remember {
                            val ms = when {
                                it < 1000000000000 -> it * 1000
                                it > 10000000000000 -> it / 1000
                                else -> it
                            }

                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = ms
                            calendar.get(Calendar.YEAR).toString()
                        }

                        Text(
                            text = year,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Normal,
                        )
                    }
                }
            }

            Text(
                text = media.overview ?: "",
                style = MaterialTheme.typography.labelMedium,
                color = LocalContentColor.current.copy(0.6f),
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
                fontWeight = FontWeight.Light,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .constrainAs(mainButtons) {
                    bottom.linkTo(divider.top, margin = 8.dp)
                },
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButtonWithLabel(
                labelId = LocaleR.string.play,
                onClick = { showLinkLoaderSheet(media) },
            ) {
                Icon(
                    painter = painterResource(UiCommonR.drawable.play),
                    contentDescription = stringResource(LocaleR.string.play),
                    modifier = Modifier.size(33.dp),
                )
            }

            IconButtonWithLabel(
                labelId = LocaleR.string.add,
                onClick = { navigateToMediaScreen(media, true) },
            ) {
                Icon(
                    painter = painterResource(R.drawable.add),
                    contentDescription = stringResource(LocaleR.string.add),
                    modifier = Modifier.size(33.dp),
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier
                .constrainAs(divider) {
                    bottom.linkTo(clickMoreButton.top)
                },
            thickness = 1.dp,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { navigateToMediaScreen(media, false) }
                .constrainAs(clickMoreButton) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom)
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(UiCommonR.drawable.info),
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
                    .weight(1F),
            )

            Icon(
                painter = painterResource(UiMobileR.drawable.right_arrow),
                contentDescription = stringResource(LocaleR.string.navigate_to_media),
                modifier = Modifier
                    .scale(0.7F)
                    .padding(end = 15.dp),
            )
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
                    radius = size / 2,
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            content()

            Text(
                text = stringResource(labelId),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Preview
@Composable
private fun MediaPreviewBottomSheetPreview() {
    FlixclusiveTheme {
        Surface {
            ModalBottomSheet(
                onDismissRequest = { },
                containerColor = Color.Transparent,
                dragHandle = null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                MediaPreviewBottomSheetContent(
                    media = DummyDataForPreview.getMedia(),
                    showMediaImageDialog = { },
                    showLinkLoaderSheet = { },
                    navigateToMediaScreen = { _, _ -> },
                )
            }
        }
    }
}
