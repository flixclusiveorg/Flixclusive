package com.flixclusive.feature.mobile.provider.test.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.dialog.ALERT_DIALOG_ROUNDNESS_PERCENTAGE
import com.flixclusive.core.ui.common.dialog.CustomBaseAlertDialog
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun RepetitiveTestNoticeDialog(
    onSkip: () -> Unit,
    onTestAgain: () -> Unit,
    onDismiss: () -> Unit,
) {
    val buttonMinHeight = 50.dp
    val buttonShape = MaterialTheme.shapes.medium
    val bottomCornerSize = CornerSize((ALERT_DIALOG_ROUNDNESS_PERCENTAGE * 2).dp)

    CustomBaseAlertDialog(
        onDismiss = onDismiss,
        action = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .padding(bottom = 10.dp)
            ) {
                Button(
                    onClick = onSkip,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.Black
                    ),
                    shape = buttonShape.copy(
                        bottomStart = bottomCornerSize,
                    ),
                    modifier = Modifier
                        .weight(1F)
                        .heightIn(min = buttonMinHeight)
                ) {
                    Text(
                        text = stringResource(id = LocaleR.string.skip),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(end = 2.dp)
                    )
                }

                Button(
                    onClick = onTestAgain,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = buttonShape.copy(
                        bottomEnd = bottomCornerSize,
                    ),
                    modifier = Modifier
                        .weight(1F)
                        .heightIn(min = buttonMinHeight)
                ) {
                    Text(
                        text = stringResource(id = LocaleR.string.re_test),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Light
                    )
                }
            }
        }
    ) {
        Text(
            text = stringResource(id = LocaleR.string.repetitive_test_warning_label),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(10.dp)
        )

        Text(
            text = stringResource(id = LocaleR.string.repetitive_test_warning_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
                .padding(horizontal = 10.dp)
        )
    }
}

@Preview
@Composable
private fun RepetitiveTestNoticeDialogPreview() {
    FlixclusiveTheme {
        Surface {
            RepetitiveTestNoticeDialog(
                onSkip = { },
                onTestAgain = { },
                onDismiss = { }
            )
        }
    }
}