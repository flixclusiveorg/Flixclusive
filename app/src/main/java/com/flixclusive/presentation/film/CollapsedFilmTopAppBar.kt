package com.flixclusive.presentation.film

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flixclusive.R
import com.flixclusive.presentation.common.IconResource
import com.flixclusive.presentation.common.UiText
import com.flixclusive.presentation.main.LABEL_START_PADDING

@Composable
fun CollapsedFilmTopAppBar(
    filmTitle: String,
    isCollapsedProvider: () -> Boolean,
    onNavigationIconClick: () -> Unit,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface

    val isCollapsed = remember(isCollapsedProvider()) { isCollapsedProvider() }

    AnimatedVisibility(
        visible = isCollapsed,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .drawBehind {
                    drawRect(surfaceColor)
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .statusBarsPadding()
                    .padding(horizontal = LABEL_START_PADDING),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigationIconClick) {
                    Icon(
                        painter = IconResource.fromDrawableResource(R.drawable.left_arrow)
                            .asPainterResource(),
                        contentDescription = UiText.StringResource(R.string.navigate_up).asString()
                    )
                }

                Text(
                    text = filmTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1F)
                        .padding(horizontal = LABEL_START_PADDING)
                )
            }
        }
    }
}