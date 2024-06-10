package com.flixclusive.core.ui.mobile.component.provider

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flixclusive.core.ui.common.util.onMediumEmphasis

@Composable
fun ButtonWithCircularProgressIndicator(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    @DrawableRes iconId: Int,
    label: String,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    emphasize: Boolean = false,
    indicatorSize: Dp = 20.dp,
    contentPadding: PaddingValues = PaddingValues(vertical = 15.dp)
) {
    if (emphasize) {
        Button(
            enabled = enabled,
            onClick = onClick,
            contentPadding = contentPadding,
            shape = MaterialTheme.shapes.medium,
            modifier = modifier,
        ) {
            AnimatedContent(
                targetState = isLoading,
                label = ""
            ) { state ->
                if (state) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .size(indicatorSize)
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = iconId),
                            contentDescription = label,
                            modifier = Modifier
                                .size(16.dp)
                        )

                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier
                                .padding(start = 3.dp)
                        )
                    }

                }
            }
        }
    } else {
        OutlinedButton(
            enabled = enabled,
            onClick = onClick,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface.onMediumEmphasis(0.8F)
            ),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.onMediumEmphasis(),
            ),
            contentPadding = contentPadding,
            shape = MaterialTheme.shapes.small,
            modifier = modifier,
        ) {
            AnimatedContent(
                targetState = isLoading,
                label = ""
            ) { state ->
                if (state) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .size(indicatorSize)
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = iconId),
                            contentDescription = label,
                            modifier = Modifier
                                .size(16.dp)
                        )

                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier
                                .padding(start = 3.dp)
                        )
                    }

                }
            }
        }
    }

}