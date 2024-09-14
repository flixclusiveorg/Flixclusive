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
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flixclusive.core.locale.UiText
import com.flixclusive.model.film.common.tv.Season
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun TvShowSeasonDropdown(
    modifier: Modifier = Modifier,
    seasons: List<Season>,
    selectedSeason: Int,
    onSeasonChange: (Int) -> Unit,
) {
    var dropdownIcon by remember { mutableIntStateOf(UiCommonR.drawable.down_arrow) }
    val isDropdownExpanded = remember { mutableStateOf(false) }

    LaunchedEffect(key1 = isDropdownExpanded.value) {
        dropdownIcon = when(isDropdownExpanded.value) {
            true -> UiCommonR.drawable.up_arrow
            false -> UiCommonR.drawable.down_arrow
        }
    }

    SeasonDropdownMenu(
        modifier = modifier,
        seasons = seasons,
        dropdownIcon = dropdownIcon,
        isDropdownExpanded = isDropdownExpanded,
        selectedSeason = selectedSeason,
        onSeasonChange = {
            if(it != selectedSeason) {
                onSeasonChange(it)
            }
        },
    )
}

@Composable
private fun SeasonDropdownMenu(
    modifier: Modifier = Modifier,
    seasons: List<Season>,
    dropdownIcon: Int,
    isDropdownExpanded: MutableState<Boolean>,
    selectedSeason: Int,
    onSeasonChange: (Int) -> Unit,
) {
    val currentSeasonName = remember(selectedSeason) {
        seasons.find { it.number == selectedSeason }
            ?.name?.run { UiText.StringValue(this) }
            ?: UiText.StringResource(LocaleR.string.unknown_season)
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
                .clickable(onClick = { isDropdownExpanded.value = true }),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = currentSeasonName.asString(),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .padding(start = 15.dp)
            )

            Spacer(
                modifier = Modifier
                    .padding(
                        horizontal = when {
                            seasons.isNotEmpty() -> 2.dp
                            else -> 7.dp
                        }
                    )
            )

            if (seasons.isNotEmpty()) {
                Icon(
                    painter = painterResource(dropdownIcon),
                    contentDescription = stringResource(LocaleR.string.down_arrow_season_dropdown_content_desc),
                    modifier = Modifier
                        .scale(0.6F)
                        .padding(end = 15.dp)
                )
            }
        }

        if (seasons.isNotEmpty()) {
            DropdownMenu(
                expanded = isDropdownExpanded.value,
                onDismissRequest = { isDropdownExpanded.value = false },
            ) {
                seasons.forEach { season ->
                    DropdownMenuItem(
                        onClick = {
                            onSeasonChange(season.number)
                            isDropdownExpanded.value = false
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
}