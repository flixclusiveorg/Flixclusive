package com.flixclusive.feature.mobile.film.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flixclusive.feature.mobile.film.R
import com.flixclusive.model.tmdb.common.tv.Season

@Composable
internal fun TvShowSeasonDropdown(
    modifier: Modifier = Modifier,
    seasons: List<Season>,
    selectedSeasonProvider: () -> Int,
    onSeasonChange: (Int) -> Unit,
) {
    var dropdownIcon by remember { mutableIntStateOf(R.drawable.down_arrow) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    val selectedSeason = remember(selectedSeasonProvider()) { selectedSeasonProvider() }

    LaunchedEffect(key1 = isDropdownExpanded) {
        dropdownIcon = when(isDropdownExpanded) {
            true -> R.drawable.up_arrow
            false -> R.drawable.down_arrow
        }
    }

    SeasonDropdownMenu(
        modifier = modifier,
        seasons = seasons,
        dropdownIconProvider = { dropdownIcon },
        isDropdownExpandedProvider = { isDropdownExpanded },
        selectedSeasonProvider = { selectedSeason },
        onSeasonChange = {
            if(it != selectedSeason) {
                onSeasonChange(it)
            }
        },
        onDropdownStateChange = { isDropdownExpanded = it }
    )
}

@Composable
fun SeasonDropdownMenu(
    modifier: Modifier = Modifier,
    seasons: List<Season>,
    dropdownIconProvider: () -> Int,
    isDropdownExpandedProvider: () -> Boolean,
    selectedSeasonProvider: () -> Int,
    onSeasonChange: (Int) -> Unit,
    onDropdownStateChange: (Boolean) -> Unit,
) {
    val selectedSeason = remember(selectedSeasonProvider()) {
        selectedSeasonProvider() - 1
    }

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .padding(horizontal = 15.dp)
            .sizeIn(
                minWidth = 112.dp,
                maxWidth = 280.dp,
                minHeight = 48.dp
            )
    ) {
        Row(
            modifier = Modifier
                .height(40.dp)
                .clickable(onClick = { onDropdownStateChange(true) }),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = seasons[selectedSeason].name,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .padding(start = 15.dp)
            )

            Spacer(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
            )

            Icon(
                painter = painterResource(dropdownIconProvider()),
                contentDescription = "A down arrow for dropdown menu",
                modifier = Modifier
                    .scale(0.6F)
                    .padding(end = 15.dp)
            )
        }

        DropdownMenu(
            expanded = isDropdownExpandedProvider(),
            onDismissRequest = { onDropdownStateChange(false) },
        ) {
            seasons.forEach { season ->
                DropdownMenuItem(
                    onClick = {
                        onSeasonChange(season.number)
                        onDropdownStateChange(false)
                    },
                    enabled = season.number != selectedSeason,
                    colors = MenuDefaults.itemColors(
                        textColor = Color.White,
                        disabledTextColor = Color.White
                    ),
                    text = {
                        Text(
                            text = season.name,
                            fontWeight = if(selectedSeason == season.number) {
                                FontWeight.Medium
                            } else FontWeight.Light
                        )
                    }
                )
            }
        }
    }
}