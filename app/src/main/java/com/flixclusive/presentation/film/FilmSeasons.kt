package com.flixclusive.presentation.film

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flixclusive.R
import com.flixclusive.domain.model.tmdb.Season
import com.flixclusive.presentation.common.IconResource
import com.flixclusive.presentation.main.LABEL_START_PADDING

@Composable
fun FilmSeasons(
    seasons: List<Season>,
    selectedSeasonProvider: () -> Int,
    onSeasonChange: (Int) -> Unit,
) {
    var dropdownIcon by remember { mutableStateOf(IconResource.fromDrawableResource(R.drawable.down_arrow)) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    val selectedSeason = remember(selectedSeasonProvider()) { selectedSeasonProvider() }

    LaunchedEffect(key1 = isDropdownExpanded) {
        dropdownIcon = when(isDropdownExpanded) {
            true -> IconResource.fromDrawableResource(R.drawable.up_arrow)
            false -> IconResource.fromDrawableResource(R.drawable.down_arrow)
        }
    }

    SeasonDropdownMenu(
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
    seasons: List<Season>,
    dropdownIconProvider: () -> IconResource,
    isDropdownExpandedProvider: () -> Boolean,
    selectedSeasonProvider: () -> Int,
    onSeasonChange: (Int) -> Unit,
    onDropdownStateChange: (Boolean) -> Unit,
) {
    val selectedSeason = remember(selectedSeasonProvider()) { selectedSeasonProvider() }

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .padding(horizontal = LABEL_START_PADDING)
            .sizeIn(
                minWidth = 112.dp,
                maxWidth = 280.dp,
                minHeight = 48.dp
            )
    ) {
        Row(
            modifier = Modifier
                .widthIn(115.dp)
                .height(40.dp)
                .drawBehind { drawRect(Color.Transparent) }
                .clickable(onClick = { onDropdownStateChange(true) }),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = "Season $selectedSeason",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .padding(start = LABEL_START_PADDING)
            )

            Icon(
                painter = dropdownIconProvider().asPainterResource(),
                contentDescription = "A down arrow for dropdown menu",
                modifier = Modifier
                    .scale(0.6F)
                    .padding(end = LABEL_START_PADDING)
            )
        }

        DropdownMenu(
            expanded = isDropdownExpandedProvider(),
            onDismissRequest = { onDropdownStateChange(false) },
        ) {
            seasons.forEach { season ->
                DropdownMenuItem(
                    onClick = {
                        onSeasonChange(season.seasonNumber)
                        onDropdownStateChange(false)
                    },
                    enabled = season.seasonNumber != selectedSeason,
                    colors = MenuDefaults.itemColors(
                        textColor = Color.White,
                        disabledTextColor = Color.White
                    ),
                    text = {
                        Text(
                            text = "Season ${season.seasonNumber}",
                            fontWeight = if(selectedSeason == season.seasonNumber) {
                                FontWeight.Medium
                            } else FontWeight.Light
                        )
                    }
                )
            }
        }
    }
}