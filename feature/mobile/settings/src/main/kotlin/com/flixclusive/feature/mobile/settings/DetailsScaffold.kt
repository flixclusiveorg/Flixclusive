package com.flixclusive.feature.mobile.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flixclusive.core.locale.R
import com.flixclusive.core.ui.common.CommonTopBar
import com.flixclusive.core.ui.common.util.ifElse

@Composable
internal fun DetailsScaffold(
    modifier: Modifier = Modifier,
    navigateBack: () -> Unit,
    isListAndDetailVisible: Boolean,
    isDetailsVisible: Boolean,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = MaterialTheme.shapes.medium
    val surface = MaterialTheme.colorScheme.surface
    val brush = Brush.verticalGradient(
        0.6F to surface,
        1F to MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    )

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            AnimatedContent(isDetailsVisible, label = "DetailsScaffold top bar") { state ->
                if (state) {
                    CommonTopBar(
                        title = stringResource(id = R.string.recently_watched),
                        onNavigate = navigateBack
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .ifElse(
                    condition = isListAndDetailVisible,
                    ifTrueModifier = Modifier
                        .padding(UserScreenHorizontalPadding)
                        .background(brush = brush, shape = shape),
                ),
            content = content
        )
    }
}