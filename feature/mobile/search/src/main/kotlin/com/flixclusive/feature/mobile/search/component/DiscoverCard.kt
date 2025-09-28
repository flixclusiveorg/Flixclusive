package com.flixclusive.feature.mobile.search.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.common.components.FilmCover
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.components.Placeholder
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.feature.mobile.search.util.SearchUiUtils

@Composable
internal fun DiscoverCard(
    label: String,
    image: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    imageSize: String = "w500",
    isProviderCatalog: Boolean = false,
    isCompanyCatalog: Boolean = false,
) {
    var showLabel by remember { mutableStateOf(true) }

    Surface(
        modifier = modifier,
        tonalElevation = 3.dp,
        shape = MaterialTheme.shapes.small,
        onClick = onClick,
    ) {
        Box {
            FilmCover.Backdrop(
                imagePath = image,
                imageSize = imageSize,
                title = label,
                modifier = Modifier.align(Alignment.Center),
                onSuccess = {
                    if (isCompanyCatalog) {
                        showLabel = false
                    }
                },
                contentScale = when {
                    !isCompanyCatalog && !isProviderCatalog -> ContentScale.FillBounds
                    else -> ContentScale.Fit
                },
            )

            if (showLabel) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                0F to Color.Transparent,
                                0.9F to Color.Black.copy(alpha = 0.8F),
                            ),
                        )
                        .matchParentSize()
                        .align(Alignment.BottomCenter),
                )

                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.asAdaptiveTextStyle(),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.BottomStart),
                )
            }
        }
    }
}

@Composable
internal fun DiscoverCardPlaceholder() {
    Placeholder(
        modifier = Modifier
            .aspectRatio(FilmCover.Backdrop.ratio)
            .width(SearchUiUtils.getCardWidth(180.dp)),
    ) {
        Placeholder(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .height(14.dp)
                .width(60.dp)
                .padding(8.dp),
        )
    }
}

@Preview
@Composable
private fun SearchItemCardPreview() {
    FlixclusiveTheme {
        Surface {
            DiscoverCard(
                image = null,
                onClick = {},
                label = "This is a very long catalog name",
                isProviderCatalog = false,
            )
        }
    }
}
