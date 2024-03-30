package com.flixclusive.core.ui.mobile.component.provider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.placeholderEffect


@Composable
fun ProviderCardPlaceholder(
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(
                    horizontal = 15.dp,
                    vertical = 10.dp
                ),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(
                    modifier = Modifier
                        .size(60.dp)
                        .placeholderEffect()
                )

                Column(
                    modifier = Modifier
                        .weight(1F),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1F)
                        ) {
                            Spacer(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .height(18.dp)
                                    .width(100.dp)
                                    .placeholderEffect()
                            )
                        }


                        Spacer(
                            modifier = Modifier
                                .height(13.dp)
                                .width(50.dp)
                                .placeholderEffect()
                        )
                    }
                    Spacer(
                        modifier = Modifier
                            .height(13.dp)
                            .width(90.dp)
                            .placeholderEffect()
                    )
                    Spacer(
                        modifier = Modifier
                            .height(13.dp)
                            .width(150.dp)
                            .placeholderEffect()
                    )
                }
            }

            Divider(
                thickness = 0.5.dp,
                modifier = Modifier
                    .padding(vertical = 15.dp)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
                    .padding(bottom = 10.dp)
            ) {
                val count = 3
                repeat(count) {
                    Spacer(
                        modifier = Modifier
                            .height(12.dp)
                            .fillMaxWidth(if (it == count - 1) 0.8F else 1F)
                            .placeholderEffect()
                    )
                }
            }

            Spacer(
                modifier = Modifier
                    .height(50.dp)
                    .fillMaxWidth()
                    .placeholderEffect(MaterialTheme.shapes.medium)
            )
        }
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