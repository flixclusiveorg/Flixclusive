package com.flixclusive.feature.mobile.settings.component

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
import com.flixclusive.core.theme.warningColor
import com.flixclusive.core.ui.common.dialog.CustomBaseAlertDialog
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun PreReleaseUpdatesWarningDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val buttonMinHeight = 50.dp
    val buttonShape = MaterialTheme.shapes.medium
    val buttonShapeRoundnessPercentage = 10

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
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = warningColor,
                        contentColor = Color.Black
                    ),
                    shape = buttonShape.copy(
                        bottomStart = CornerSize((buttonShapeRoundnessPercentage  *2).dp),
                    ),
                    modifier = Modifier
                        .weight(1F)
                        .heightIn(min = buttonMinHeight)
                ) {
                    Text(
                        text = stringResource(id = LocaleR.string.opt_in_prerelease),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(end = 2.dp)
                    )
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant.onMediumEmphasis()
                    ),
                    shape = buttonShape.copy(
                        bottomEnd = CornerSize((buttonShapeRoundnessPercentage  *2).dp),
                    ),
                    modifier = Modifier
                        .weight(1F)
                        .heightIn(min = buttonMinHeight)
                ) {
                    Text(
                        text = stringResource(id = LocaleR.string.opt_out_prerelease),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Light
                    )
                }
            }
        }
    ) {
        Text(
            text = "\uD83D\uDDFF\uD83D\uDDFF\uD83D\uDDFF\uD83D\uDDFF",
            color = warningColor,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(10.dp)
        )

        Text(
            text = stringResource(LocaleR.string.warning_use_prerelease_updates),
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
private fun PreReleaseUpdatesWarningDialogPreview() {
    FlixclusiveTheme {
        Surface {
            PreReleaseUpdatesWarningDialog(
                onConfirm = {},
                onDismiss = {}
            )
        }
    }
}