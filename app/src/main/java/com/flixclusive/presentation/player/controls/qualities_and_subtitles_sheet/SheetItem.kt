package com.flixclusive.presentation.player.controls.qualities_and_subtitles_sheet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flixclusive.presentation.common.composables.applyDropShadow

@Composable
fun SheetItem(
    name: String,
    index: Int,
    selectedIndex: Int,
    onClick: (Int, String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clickable(enabled = index != selectedIndex) {
                onClick(index, name)
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            val isSelected = selectedIndex == index
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Check indicator for selected video server",
                    modifier = Modifier.padding(start = 25.dp)
                )
            }

            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge.applyDropShadow(),
                modifier = Modifier.padding(start = 60.dp),
                fontWeight = if (!isSelected) FontWeight.Light else FontWeight.Medium,
                color = Color.White
            )
        }
    }
}