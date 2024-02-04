package com.flixclusive.feature.tv.search.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceColors
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ClickableSurfaceShape
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.tv.util.focusOnMount

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun KeyboardButton(
    modifier: Modifier = Modifier,
    itemKey: String,
    enabled: Boolean = true,
    size: ButtonSize = ButtonSize(KeyboardCellSize),
    colors: ClickableSurfaceColors = ClickableSurfaceDefaults.colors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = LocalContentColor.current.onMediumEmphasis(),
        focusedContainerColor = MaterialTheme.colorScheme.inverseSurface,
        focusedContentColor = MaterialTheme.colorScheme.inverseOnSurface
    ),
    shape: ClickableSurfaceShape = ClickableSurfaceDefaults.shape(
        shape = MaterialTheme.shapes.extraSmall
    ),
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        enabled = enabled,
        onClick = onClick,
        onLongClick = onLongClick,
        colors = colors,
        shape = shape,
        modifier = Modifier
            .focusOnMount(itemKey = itemKey)
            .size(size.width, size.height)
            .padding(3.dp)
            .then(modifier)
    ) {
        content()
    }
}