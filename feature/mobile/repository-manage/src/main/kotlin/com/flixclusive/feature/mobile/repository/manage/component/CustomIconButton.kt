package com.flixclusive.feature.mobile.repository.manage.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomIconButton(
    description: String,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    // TODO: Migrate to PlainTooltipBox
    val tooltipState = rememberTooltipState(isPersistent = true)
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        state = tooltipState,
        tooltip = {
            PlainTooltip {
                Text(text = description)
            }
        },
    ) {
        Box(
            modifier =
                Modifier
                    .size(35.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}
