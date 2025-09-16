package com.flixclusive.feature.mobile.film.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.components.Placeholder
import com.flixclusive.core.presentation.mobile.extensions.isCompact
import com.flixclusive.core.presentation.mobile.extensions.isMedium
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.MobileUiUtil.DefaultScreenPaddingHorizontal
import com.flixclusive.feature.mobile.film.getBackdropAspectRatio

@Composable
internal fun FilmScreenPlaceholder() {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val usePortraitView = windowSizeClass.windowWidthSizeClass.isCompact ||
        windowSizeClass.windowWidthSizeClass.isMedium

    val surface = MaterialTheme.colorScheme.surface
    val backdropAspectRatio = remember(usePortraitView) {
        getBackdropAspectRatio(usePortraitView)
    }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier.fillMaxSize()
    ) {
        // Backdrop placeholder
        Placeholder(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(backdropAspectRatio)
                .drawWithContent {
                    drawContent()
                    drawRect(
                        Brush.verticalGradient(
                            0F to Color.Transparent,
                            0.9F to surface,
                        ),
                    )
                },
        )

        Column(
            verticalArrangement = Arrangement.Bottom,
            modifier = Modifier
                .aspectRatio(backdropAspectRatio * 0.95f)
                .padding(horizontal = DefaultScreenPaddingHorizontal),
        ) {
            BriefDetailsPlaceholder()

            GenresPlaceholder()

            HeaderButtonsPlaceholder()

            DescriptionPlaceholder()
        }
    }
}

@Composable
private fun BriefDetailsPlaceholder() {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom),
    ) {
        Placeholder(
            modifier = Modifier
                .fillMaxWidth(0.2f)
                .height(10.dp),
        )

        Placeholder(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .height(20.dp),
        )

        Placeholder(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(10.dp),
        )
    }
}

@Composable
private fun GenresPlaceholder() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth(0.38f)
            .padding(vertical = 8.dp)
            .padding(top = 4.dp)
    ) {
        Placeholder(
            modifier = Modifier
                .weight(0.5f)
                .height(16.dp),
        )

        Placeholder(
            modifier = Modifier
                .weight(0.5f)
                .height(16.dp),
        )
    }
}

@Composable
private fun HeaderButtonsPlaceholder() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Box(modifier = Modifier.weight(1f)) {
            Placeholder(
                modifier = Modifier
                    .width(150.dp)
                    .height(36.dp),
            )
        }

        Placeholder(modifier = Modifier.size(36.dp))

        Placeholder(modifier = Modifier.size(36.dp))
    }
}

@Composable
private fun DescriptionPlaceholder() {
    Column(
        verticalArrangement = Arrangement.spacedBy(1.dp),
        modifier = Modifier
            .padding(top = 10.dp)
    ) {
        repeat(3) {
            Placeholder(
                modifier = Modifier
                    .fillMaxWidth(
                        if (it == 2) 0.7f
                        else 1f
                    )
                    .padding(vertical = 4.dp)
                    .height(10.dp),
            )
        }
    }
}

@Preview
@Composable
private fun FilmScreenPlaceholderPreview() {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            FilmScreenPlaceholder()
        }
    }
}
