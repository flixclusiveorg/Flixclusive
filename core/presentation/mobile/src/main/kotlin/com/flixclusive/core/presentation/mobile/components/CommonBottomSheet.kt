package com.flixclusive.core.presentation.mobile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetDefaults
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.theme.MobileColors.surfaceColorAtElevation

/**
 * A common bottom sheet component that can be reused across the app.
 * */
@Composable
fun CommonBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    shape: Shape = MaterialTheme.shapes.small.copy(
        bottomEnd = CornerSize(0.dp),
        bottomStart = CornerSize(0.dp),
    ),
    containerColor: Color = MaterialTheme.colorScheme.surfaceColorAtElevation(level = 1),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    tonalElevation: Dp = 1.dp,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    dragHandle: @Composable (() -> Unit)? = { CommonDragHandle() },
    contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.windowInsets },
    properties: ModalBottomSheetProperties = ModalBottomSheetDefaults.properties,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        sheetMaxWidth = sheetMaxWidth,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        scrimColor = scrimColor,
        dragHandle = dragHandle,
        contentWindowInsets = contentWindowInsets,
        properties = properties,
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            content()
        }
    }
}

/** A common drag handle for bottom sheets. */
@Composable
fun CommonDragHandle(
    modifier: Modifier = Modifier,
    width: Dp = 50.dp,
    height: Dp = 6.dp,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    color: Color = LocalContentColor.current.copy(0.6f),
) {
    Box(
        modifier
            .padding(top = 20.dp, bottom = 15.dp)
            .size(width = width, height = height)
            .background(color = color, shape = shape),
    )
}

@Preview
@Composable
private fun CommonBottomSheetPreview() {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            CommonBottomSheet(onDismissRequest = { }) {
                Text("Hello, World!")
                Text("The quick brown fox jumps over the lazy dog.")
            }
        }
    }
}
