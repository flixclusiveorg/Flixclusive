package com.flixclusive.feature.tv.search.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceColors
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ClickableSurfaceShape
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import com.flixclusive.core.ui.common.util.onMediumEmphasis
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun KeyboardButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: ButtonSize = ButtonSize(KeyboardCellSize),
    colors: ClickableSurfaceColors = ClickableSurfaceDefaults.colors(
        containerColor = Color.Transparent,
        contentColor = LocalContentColor.current.onMediumEmphasis(),
        focusedContainerColor = MaterialTheme.colorScheme.inverseSurface,
        focusedContentColor = MaterialTheme.colorScheme.inverseOnSurface
    ),
    shape: ClickableSurfaceShape = ClickableSurfaceDefaults.shape(
        shape = RectangleShape
    ),
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        enabled = enabled,
        onClick = onClick,
        colors = colors,
        shape = shape,
        modifier = Modifier
            .size(size.width, size.height)
            .padding(3.dp)
            .then(modifier)
    ) {
        content()
    }
}