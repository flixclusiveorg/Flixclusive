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
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.R
import com.flixclusive.domain.model.provider.SourceProviderDetails

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderItemCard(
    provider: SourceProviderDetails,
    displacementOffset: Float?,
    onToggleProvider: () -> Unit,
) {
    val isBeingDragged = remember(displacementOffset) {
        displacementOffset != null
    }

    val color = if (isBeingDragged && !provider.isMaintenance) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = Modifier
            .graphicsLayer { translationY = displacementOffset ?: 0f }
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        Card(
            enabled = !provider.isMaintenance,
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = color,
                contentColor = contentColorFor(backgroundColor = color)
            ),
            border = if (isBeingDragged && provider.isMaintenance)
                BorderStroke(
                    width = 2.dp,
                    color = contentColorFor(color)
                ) else null,
            onClick = {
                onToggleProvider()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    enabled = !provider.isMaintenance,
                    checked = !provider.isIgnored && !provider.isMaintenance,
                    onCheckedChange = {
                        onToggleProvider()
                    },
                    colors = CheckboxDefaults.colors(
                        uncheckedColor = contentColorFor(color)
                    ),
                    modifier = Modifier
                        .padding(2.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1F)
                        .padding(horizontal = 2.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = provider.provider.name,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal
                        ),
                    )

                    if (provider.isMaintenance) {
                        Text(
                            text = stringResource(id = R.string.maintenance_all_caps),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                letterSpacing = 2.sp,
                            ),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                Icon(
                    painter = painterResource(id = R.drawable.round_drag_indicator_24),
                    contentDescription = "Drag indicator for provider card",
                    modifier = Modifier
                        .padding(2.dp)
                        .padding(end = 5.dp)
                )
            }
        }
    }
}