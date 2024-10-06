package com.flixclusive.feature.mobile.player.controls.common

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.flixclusive.core.ui.common.util.noIndicationClickable

@Composable
internal fun BasePopupScreen(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    BackHandler {
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.9F))
            .then(modifier)
    ) {
        // Block touches
        Box(
            modifier = Modifier
                .fillMaxSize()
                .noIndicationClickable { }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.TopStart),
            verticalArrangement = Arrangement.spacedBy(15.dp),
        ) {
            content()
        }
    }
}