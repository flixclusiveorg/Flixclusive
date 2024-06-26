package com.flixclusive.feature.mobile.search.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.theme.lightGrayElevated
import com.flixclusive.core.ui.common.FilmCover
import com.flixclusive.core.ui.common.util.ifElse
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.common.util.placeholderEffect


@Composable
internal fun SearchItemCard(
    image: String?,
    label: String? = null,
    isProviderCatalog: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .padding(10.dp),
        tonalElevation = 3.dp,
        shape = MaterialTheme.shapes.extraSmall,
        onClick = onClick
    ) {
        Box {
            FilmCover.Backdrop(
                imagePath = image,
                imageSize = if (label != null) "w500" else "w500_filter(negate,000,666)",
                contentDescription = label,
                showPlaceholder = false,
                contentScale = if (label != null && !isProviderCatalog) ContentScale.FillBounds else ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.Center)
                    .ifElse(
                        condition = label == null || isProviderCatalog,
                        ifTrueModifier = Modifier
                            .height(80.dp)
                            .padding(10.dp)
                    )
            )

            if (label != null) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                0F to Color.Transparent,
                                0.9F to Color.Black.onMediumEmphasis(emphasis = 0.8F)
                            ),
                        )
                        .matchParentSize()
                        .align(Alignment.BottomCenter),
                )

                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.BottomStart)
                )
            }
        }
    }
}

@Composable
internal fun SearchItemCardPlaceholderWithText() {
    Box(
        modifier = Modifier
            .height(130.dp)
            .width(190.dp)
            .padding(10.dp)
            .placeholderEffect(shape = MaterialTheme.shapes.medium)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 8.dp,
                    bottom = 8.dp,
                ),
            contentAlignment = Alignment.BottomStart
        ) {
            Spacer(
                modifier = Modifier
                    .height(14.dp)
                    .width(60.dp)
                    .graphicsLayer {
                        shape = RoundedCornerShape(100)
                        clip = true
                    }
                    .drawBehind {
                        drawRect(lightGrayElevated)
                    }
            )
        }
    }
}

@Preview
@Composable
private fun SearchItemCardPreview() {
    FlixclusiveTheme {
        Surface {
            SearchItemCard(image = null) {

            }
        }
    }
}