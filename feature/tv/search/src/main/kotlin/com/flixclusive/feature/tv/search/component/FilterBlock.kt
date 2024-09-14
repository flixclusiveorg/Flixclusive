package com.flixclusive.feature.tv.search.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.locale.R as LocaleR

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun FilterBlock(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    name: String,
    onClick: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "")

    val borderDp by infiniteTransition.animateValue(
        initialValue = 0.dp,
        targetValue = 3.dp,
        typeConverter = Dp.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    Surface(
        modifier = modifier,
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.onSurface.onMediumEmphasis()
                ),
                shape = MaterialTheme.shapes.large
            ),
            focusedBorder = Border(
                border = BorderStroke(
                    width = borderDp,
                    color = MaterialTheme.colorScheme.primary
                ),
                shape = MaterialTheme.shapes.large
            )
        ),
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.large),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1F),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = if (isSelected) LocalContentColor.current else LocalContentColor.current.onMediumEmphasis(emphasis = 0.8F),
            focusedContainerColor = Color.Transparent,
            focusedContentColor = Color.White
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(
                    vertical = 5.dp,
                    horizontal = 16.dp
                )
                .align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(visible = isSelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = stringResource(LocaleR.string.check_indicator_content_desc),
                    modifier = Modifier
                        .size(16.dp)
                )
            }

            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}