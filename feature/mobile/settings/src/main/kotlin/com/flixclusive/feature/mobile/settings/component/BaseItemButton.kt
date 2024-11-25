package com.flixclusive.feature.mobile.settings.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.ui.common.util.onMediumEmphasis

@Composable
internal fun BaseItemButton(
    title: String,
    description: String?,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val color by animateColorAsState(
        targetValue = if (enabled) Color.White else Color.Gray,
        label = ""
    )

    CompositionLocalProvider(
        LocalContentColor provides color
    ) {
        Box(
            modifier = Modifier
                .clickable(
                    enabled = enabled,
                    onClick = onClick
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .padding(
                        horizontal = 15.dp,
                        vertical = 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(
                        space = 3.dp,
                        alignment = Alignment.CenterVertically
                    ),
                    modifier = Modifier
                        .weight(1F)
                        .padding(end = 15.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Normal,
                            fontSize = 16.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                    )

                    description?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = LocalContentColor.current.onMediumEmphasis()
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                content()
            }
        }
    }
}