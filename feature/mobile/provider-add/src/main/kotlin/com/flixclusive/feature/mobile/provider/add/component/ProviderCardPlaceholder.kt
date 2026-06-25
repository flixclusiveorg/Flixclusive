package com.flixclusive.feature.mobile.provider.add.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.common.theme.Elevations
import com.flixclusive.core.presentation.mobile.components.Placeholder
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme

@Composable
internal fun ProviderCardPlaceholder(modifier: Modifier = Modifier) {
    val elevation = Elevations.LEVEL_5

    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                shape = MaterialTheme.shapes.small,
            ).fillMaxWidth()
            .padding(
                horizontal = 15.dp,
                vertical = 10.dp,
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Placeholder(
                elevation = elevation,
                modifier = Modifier
                    .size(60.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1F),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1F),
                    ) {
                        Placeholder(
                            elevation = elevation,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .height(18.dp)
                                .width(100.dp)
                        )
                    }

                    Placeholder(
                        elevation = elevation,
                        modifier = Modifier
                            .height(13.dp)
                            .width(50.dp)
                    )
                }
                Placeholder(
                    elevation = elevation,
                    modifier = Modifier
                        .height(13.dp)
                        .width(90.dp)
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier
                .padding(vertical = 15.dp),
            thickness = 0.5.dp,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            val count = 3
            repeat(count) {
                Placeholder(
                    elevation = elevation,
                    modifier = Modifier
                        .height(12.dp)
                        .fillMaxWidth(if (it == count - 1) 0.8F else 1F)
                )
            }
        }

        Placeholder(
            elevation = elevation,
            modifier = Modifier
                .height(50.dp)
                .fillMaxWidth()
        )
    }
}

@Preview
@Composable
private fun ProviderCardPreview() {
    FlixclusiveTheme {
        Surface {
            ProviderCardPlaceholder()
        }
    }
}
