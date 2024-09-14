package com.flixclusive.core.ui.mobile.component.film

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.model.film.util.FilmType
import com.flixclusive.core.locale.R as LocaleR

@Composable
fun FilmTypeFilters(
    currentFilterSelected: FilmType,
    onFilterChange: (FilmType) -> Unit,
) {
    Column {
        Text(
            text = stringResource(LocaleR.string.film_filter),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp)
                .padding(top = 15.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            FilmType.entries.forEach { filter ->
                val buttonColors = when (currentFilterSelected == filter) {
                    true -> ButtonDefaults.outlinedButtonColors(
                        disabledContainerColor = MaterialTheme.colorScheme.primary,
                        disabledContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                    false -> ButtonDefaults.outlinedButtonColors(
                        contentColor = LocalContentColor.current.onMediumEmphasis()
                    )
                }

                Box(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .padding(vertical = 12.dp)
                ) {
                    OutlinedButton(
                        onClick = { onFilterChange(filter) },
                        enabled = currentFilterSelected != filter,
                        colors = buttonColors,
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier
                            .height(35.dp)
                    ) {
                        Text(
                            text = stringResource(filter.stringId),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

private val FilmType.stringId: Int
    get() = when (this) {
        FilmType.MOVIE -> LocaleR.string.movie
        FilmType.TV_SHOW -> LocaleR.string.tv_show
    }