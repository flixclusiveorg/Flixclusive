package com.flixclusive.feature.mobile.repository.manage.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.components.material3.PlainTooltipBox
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomIconButton(
    description: String,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    PlainTooltipBox(description = description) {
        Box(
            modifier = Modifier
                .size(getAdaptiveDp(35.dp))
                .clip(CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}
