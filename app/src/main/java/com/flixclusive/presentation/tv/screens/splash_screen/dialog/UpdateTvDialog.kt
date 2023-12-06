package com.flixclusive.presentation.tv.screens.splash_screen.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.flixclusive.R
import com.flixclusive.presentation.mobile.utils.ComposeMobileUtils.colorOnMediumEmphasisMobile
import com.flixclusive.presentation.tv.utils.ComposeTvUtils.colorOnMediumEmphasisTv
import com.flixclusive.presentation.tv.utils.ModifierTvUtils.focusOnInitialVisibility

@Composable
fun UpdateTvDialog(
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    val dialogColor = MaterialTheme.colorScheme.surface
    val dialogShape = RoundedCornerShape(12.dp)
    val buttonMinHeight = 60.dp

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .height(280.dp)
                .fillMaxWidth(0.65F)
                .graphicsLayer {
                    shape = dialogShape
                    clip = true
                }
                .drawBehind {
                    drawRect(dialogColor)
                }
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.new_update_header),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                )

                Text(
                    text = stringResource(R.string.new_update_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorOnMediumEmphasisMobile(Color.White),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1F)
                        .padding(horizontal = 5.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = colorOnMediumEmphasisTv(Color.White)
                        ),
                        shape = ButtonDefaults.shape(
                            shape = MaterialTheme.shapes.medium
                        ),
                        scale = ButtonDefaults.scale(focusedScale = 1F),
                        modifier = Modifier
                            .weight(1F)
                            .heightIn(min = buttonMinHeight)
                            .padding(5.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.not_now_label),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Light,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }

                    Button(
                        onClick = {
                            onUpdate()
                            onDismiss()
                        },
                        colors = ButtonDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = ButtonDefaults.shape(
                            shape = MaterialTheme.shapes.medium
                        ),
                        scale = ButtonDefaults.scale(focusedScale = 1F),
                        modifier = Modifier
                            .weight(1F)
                            .heightIn(min = buttonMinHeight)
                            .padding(5.dp)
                            .focusOnInitialVisibility()
                    ) {
                        Text(
                            text = stringResource(R.string.update_label),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }
                }

            }
        }
    }
}