package com.flixclusive.presentation.mobile.common.composables.film

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flixclusive.R
import com.flixclusive.presentation.mobile.main.LABEL_START_PADDING

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
                        painter = painterResource(R.drawable.left_arrow),
                        contentDescription = stringResource(R.string.navigate_up)
                    )
                }

                Text(
                    text = filmTitle,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
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