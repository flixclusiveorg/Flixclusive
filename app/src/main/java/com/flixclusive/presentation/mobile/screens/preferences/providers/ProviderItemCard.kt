package com.flixclusive.presentation.mobile.screens.preferences.providers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.R
import com.flixclusive.domain.model.provider.SourceProviderDetails

@Composable
fun ProviderItemCard(
    provider: SourceProviderDetails,
    displacementOffset: Float?,
    onToggleProvider: () -> Unit,
) {
    val color = MaterialTheme.colorScheme.surfaceVariant

    val isBeingDragged = remember(displacementOffset) {
        displacementOffset != null
    }

    Column(
        modifier = Modifier
            .graphicsLayer { translationY = displacementOffset ?: 0f }
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = color.copy(alpha = if (isBeingDragged) 0.9F else 1F),
                contentColor = contentColorFor(backgroundColor = color)
            ),
            border = if (isBeingDragged)
                BorderStroke(
                    width = 2.dp,
                    color = contentColorFor(color)
                ) else null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = !provider.isIgnored,
                    onCheckedChange = {
                        onToggleProvider()
                    },
                    modifier = Modifier
                        .padding(2.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1F)
                        .padding(2.dp)
                ) {
                    Text(
                        text = provider.source.name,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal
                        ),
                    )
                }

                Icon(
                    painter = painterResource(id = R.drawable.round_drag_indicator_24),
                    contentDescription = "Drag indicator for provider card",
                    modifier = Modifier
                        .padding(2.dp)
                )
            }
        }
    }
}