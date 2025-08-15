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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.database.entity.DBFilm
import com.flixclusive.core.presentation.theme.FlixclusiveTheme
import com.flixclusive.core.strings.UiText
import com.flixclusive.core.ui.common.util.CoilUtil.ProvideAsyncImagePreviewHandler
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveTextStyle
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.core.ui.common.util.adaptive.TextStyleMode
import com.flixclusive.core.ui.common.util.adaptive.TypographyStyle
import com.flixclusive.core.ui.mobile.util.getFeedbackOnLongPress
import com.flixclusive.feature.mobile.library.manage.PreviewPoster
import com.flixclusive.feature.mobile.library.manage.PreviewPoster.Companion.toPreviewPoster
import com.flixclusive.core.strings.R as LocaleR

internal val DefaultLibraryCardShape = RoundedCornerShape(4.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LibraryCard(
    previews: List<PreviewPoster>,
    itemsCount: Int,
    name: UiText,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: UiText? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val hapticFeedback = getFeedbackOnLongPress()

    Box(
        modifier =
            modifier
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
                ),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(25.dp),
        ) {
            StackedPosters(
                previews = previews,
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
                        text = name.asString(),
                        style =
                            getAdaptiveTextStyle(
                                mode = TextStyleMode.Emphasized,
                                style = TypographyStyle.Label,
                                increaseBy = 2.sp,
                            ),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 3,
                    )

                    if (description != null) {
                        Text(
                            text = description.asString(),
                            style =
                                getAdaptiveTextStyle(
                                    mode = TextStyleMode.Normal,
                                    style = TypographyStyle.Body,
                                    increaseBy = 2.sp,
                                ),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 2,
                        )
                    }

                    Text(
                        text =
                            context.resources.getQuantityString(
                                LocaleR.plurals.number_of_items_format,
                                itemsCount,
                                itemsCount,
                            ),
                        style =
                            getAdaptiveTextStyle(
                                mode = TextStyleMode.NonEmphasized,
                                style = TypographyStyle.Body,
                                increaseBy = 2.sp,
                            ),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 5,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun LibraryCardBasePreview() {
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
                    items(20) { i ->
                        LibraryCard(
                            onClick = {},
                            onLongClick = {},
                            name = UiText.from("Test library $i"),
                            description =
                                UiText.from(
                                    """
                                    Lorem ipsum Lorem ipsum Lorem ipsum Lorem ipsum Lorem ipsum Lorem ipsum Lorem ipsum
                                    """.trimIndent(),
                                ),
                            itemsCount = 1,
                            previews = List(3) { DBFilm(title = "Film #$it").toPreviewPoster() },
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
