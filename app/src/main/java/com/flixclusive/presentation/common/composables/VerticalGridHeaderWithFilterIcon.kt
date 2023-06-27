package com.flixclusive.presentation.common.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flixclusive.R
import com.flixclusive.domain.model.tmdb.FilmType
import com.flixclusive.presentation.common.IconResource
import com.flixclusive.presentation.common.UiText
import com.flixclusive.presentation.main.LABEL_START_PADDING

@Composable
fun VerticalGridHeaderWithFilterIcon(
    headerTitle: String,
    shouldOpenFilterSheet: Boolean,
    currentFilterSelected: FilmType,
    onNavigationIconClick: (() -> Unit)? = null,
    onFilterChange: (FilmType) -> Unit,
    onFilterClick: () -> Unit,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface

    Column(
        modifier = Modifier
            .drawBehind {
                drawRect(surfaceColor)
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(65.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if(onNavigationIconClick != null) {
                IconButton(onClick = onNavigationIconClick) {
                    Icon(
                        painter = IconResource.fromDrawableResource(R.drawable.left_arrow)
                            .asPainterResource(),
                        contentDescription = UiText.StringResource(R.string.navigate_up).asString()
                    )
                }
            }

            Text(
                text = headerTitle,
                style = MaterialTheme.typography.headlineMedium,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                modifier = Modifier
                    .weight(1F)
                    .padding(start = LABEL_START_PADDING)
            )

            IconButton(onClick = onFilterClick) {
                Icon(
                    painter = IconResource.fromDrawableResource(R.drawable.filter)
                        .asPainterResource(),
                    contentDescription = UiText.StringResource(R.string.filter_button).asString(),
                    modifier = Modifier
                        .padding(end = LABEL_START_PADDING)
                )
            }
        }

        AnimatedVisibility(
            visible = shouldOpenFilterSheet
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawRect(surfaceColor)
                    }
            ) {
                FilmTypeFilters(
                    currentFilterSelected = currentFilterSelected,
                    onFilterChange = onFilterChange
                )
            }
        }
    }
}


@Composable
fun VerticalGridHeader(
    headerTitle: String,
    onNavigationIconClick: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .padding(bottom = 25.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if(onNavigationIconClick != null) {
                IconButton(onClick = { onNavigationIconClick() }) {
                    Icon(
                        painter = IconResource.fromDrawableResource(R.drawable.left_arrow)
                            .asPainterResource(),
                        contentDescription = UiText.StringResource(R.string.navigate_up).asString()
                    )
                }
            }

            Text(
                text = headerTitle,
                style = MaterialTheme.typography.headlineMedium,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                modifier = Modifier
                    .weight(1F)
                    .padding(start = LABEL_START_PADDING)
            )
        }
    }
}