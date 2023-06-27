package com.flixclusive.presentation.player.controls.episodes_sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flixclusive.R
import com.flixclusive.presentation.common.IconResource
import com.flixclusive.presentation.common.composables.applyDropShadow


@Composable
fun SeasonHeader(
    modifier: Modifier = Modifier,
    headerLabel: String,
    shouldShowBackIcon: Boolean,
    onClick: () -> Unit,
    onBackIconClick: () -> Unit,
    onDismissIconClick: () -> Unit,
) {
    val headerIconToUse = remember(shouldShowBackIcon) {
        if(shouldShowBackIcon) {
            IconResource.fromDrawableResource(R.drawable.left_arrow) to "Back button to go back to episodes list"
        } else IconResource.fromImageVector(Icons.Rounded.Close) to "Close button for episodes sheet"
    }

    Row(
        modifier = modifier
            .background(Color.White.copy(0.1F), RoundedCornerShape(10))
            .clickable {
                if (!shouldShowBackIcon)
                    onClick()
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = headerIconToUse.first.asPainterResource(),
            contentDescription = headerIconToUse.second,
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .clickable {
                    if (shouldShowBackIcon) {
                        onBackIconClick()
                    } else onDismissIconClick()
                }
        )

        Text(
            text = headerLabel,
            style = MaterialTheme.typography.labelLarge.applyDropShadow(),
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1F),
            color = Color.White
        )
    }
}