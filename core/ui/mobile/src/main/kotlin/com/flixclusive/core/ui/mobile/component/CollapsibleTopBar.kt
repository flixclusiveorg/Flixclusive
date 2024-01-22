package com.flixclusive.core.ui.mobile.component

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.util.R as UtilR

@Composable
fun CollapsibleTopBar(
    title: String,
    isCollapsedProvider: () -> Boolean,
    onNavigationIconClick: () -> Unit,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface

    val isCollapsed by rememberUpdatedState(newValue = isCollapsedProvider())

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
                    .padding(horizontal = 15.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigationIconClick) {
                    Icon(
                        painter = painterResource(UiCommonR.drawable.left_arrow),
                        contentDescription = stringResource(UtilR.string.navigate_up)
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1F)
                        .padding(horizontal = 15.dp)
                )
            }
        }
    }
}