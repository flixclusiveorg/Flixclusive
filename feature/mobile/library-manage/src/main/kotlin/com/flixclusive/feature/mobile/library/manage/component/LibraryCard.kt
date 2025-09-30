package com.flixclusive.feature.mobile.library.manage.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.presentation.common.components.ProvideAsyncImagePreviewHandler
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.util.getFeedbackOnLongPress
import com.flixclusive.feature.mobile.library.manage.LibraryListWithPreview
import com.flixclusive.feature.mobile.library.manage.PreviewPoster.Companion.toPreviewPoster
import com.flixclusive.core.strings.R as LocaleR

internal val DefaultLibraryCardShape = RoundedCornerShape(4.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LibraryCard(
    libraryListWithPreview: LibraryListWithPreview,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val hapticFeedback = getFeedbackOnLongPress()

    Row(
        modifier = modifier
            .height(getAdaptiveDp(150.dp, 20.dp))
            .clip(DefaultLibraryCardShape)
            .combinedClickable(
                onLongClick = {
                    onLongClick?.let {
                        hapticFeedback()
                        it.invoke()
                    }
                },
                onClick = onClick,
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(25.dp),
    ) {
        StackedPosters(
            previews = libraryListWithPreview.previews,
            modifier = Modifier.weight(0.25f),
        )

        Box(
            modifier =
                Modifier
                    .weight(0.5f)
                    .fillMaxHeight(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Text(
                    text = libraryListWithPreview.name,
                    style = MaterialTheme.typography.labelLarge.asAdaptiveTextStyle(),
                    fontWeight = FontWeight.Bold,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 3,
                )

                if (libraryListWithPreview.description != null) {
                    Text(
                        text = libraryListWithPreview.description!!,
                        style = MaterialTheme.typography.bodySmall.asAdaptiveTextStyle(),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 2,
                    )
                }

                Text(
                    text = context.resources.getQuantityString(
                        LocaleR.plurals.number_of_items_format,
                        libraryListWithPreview.itemsCount,
                        libraryListWithPreview.itemsCount,
                    ),
                    style = MaterialTheme.typography.bodySmall.asAdaptiveTextStyle(),
                    color = LocalContentColor.current.copy(0.6f),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 5,
                )
            }
        }
    }
}

@Preview
@Composable
private fun LibraryCardBasePreview() {
    val films = remember {
        List(3) {
            DummyDataForPreview
                .getMovie(id = "$it")
                .toPreviewPoster()
        }
    }

    val items = remember {
        List(20) {
            LibraryListWithPreview(
                list = LibraryList(
                    id = it,
                    ownerId = 0,
                    name = "My List #$it",
                    description = "This is my favorite list number $it",
                ),
                previews = films,
                itemsCount = films.size,
            )
        }
    }

    FlixclusiveTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            ProvideAsyncImagePreviewHandler(
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
            ) {
                LazyVerticalGrid(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    columns =
                        GridCells.Adaptive(
                            getAdaptiveDp(
                                compact = 300.dp,
                                medium = 350.dp,
                                expanded = 400.dp,
                            ),
                        ),
                ) {
                    items(items) {
                        LibraryCard(
                            libraryListWithPreview = it,
                            onClick = {},
                            onLongClick = {},
                        )
                    }
                }
            }
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun LibraryCardCompactLandscapePreview() {
    LibraryCardBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun LibraryCardMediumPortraitPreview() {
    LibraryCardBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun LibraryCardMediumLandscapePreview() {
    LibraryCardBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun LibraryCardExtendedPortraitPreview() {
    LibraryCardBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun LibraryCardExtendedLandscapePreview() {
    LibraryCardBasePreview()
}
