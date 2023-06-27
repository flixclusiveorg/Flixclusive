package com.flixclusive.presentation.player.controls.episodes_sheet

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


@Composable
fun SheetSeasonItem(
    modifier: Modifier = Modifier,
    seasonNumber: Int,
    selectedSeason: Int,
    onSeasonChange: (Int) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clickable(enabled = selectedSeason != seasonNumber) {
                onSeasonChange(seasonNumber)
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            val isSelected = selectedSeason == seasonNumber
            if(isSelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Check indicator for selected season",
                    modifier = Modifier.padding(start = 25.dp)
                )
            }

            Text(
                text = "Season $seasonNumber",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 60.dp),
                fontWeight = if(!isSelected) FontWeight.Light else FontWeight.Medium
            )
        }
    }
}