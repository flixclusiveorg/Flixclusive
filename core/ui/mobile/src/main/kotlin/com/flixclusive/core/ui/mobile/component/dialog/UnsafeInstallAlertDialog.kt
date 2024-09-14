package com.flixclusive.core.ui.mobile.component.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.dialog.ALERT_DIALOG_ROUNDNESS_PERCENTAGE
import com.flixclusive.core.ui.common.dialog.CustomBaseAlertDialog
import com.flixclusive.core.ui.common.util.noIndicationClickable
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.mobile.component.CustomCheckbox
import com.flixclusive.core.locale.R as LocaleR

@Composable
fun UnsafeInstallAlertDialog(
    quantity: Int,
    formattedName: Any,
    warnOnInstall: Boolean,
    onConfirm: (disableWarning: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var checkboxState by remember { mutableStateOf(!warnOnInstall) }

    val buttonMinHeight = 50.dp
    val buttonShape = MaterialTheme.shapes.medium
    val cornerSize = CornerSize(
        (ALERT_DIALOG_ROUNDNESS_PERCENTAGE * 2).dp
    )

    val context = LocalContext.current
    val message = context.resources.getQuantityString(
        LocaleR.plurals.warning_install_message_first_half, quantity, formattedName
    ) + " " + context.getString(LocaleR.string.warning_install_message_second_half)

    CustomBaseAlertDialog(
        onDismiss = onDismiss,
        dialogProperties = DialogProperties(
            dismissOnClickOutside = false
        ),
        action = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .padding(bottom = 10.dp)
            ) {
                Button(
                    onClick = {
                        val isDisablingWarnings = !checkboxState

                        onConfirm(isDisablingWarnings)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.Black
                    ),
                    shape = buttonShape.copy(
                        bottomStart = cornerSize,
                    ),
                    modifier = Modifier
                        .weight(1F)
                        .heightIn(min = buttonMinHeight)
                ) {
                    Text(
                        text = stringResource(id = LocaleR.string.proceed),
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
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = buttonShape.copy(
                        bottomEnd = cornerSize,
                    ),
                    modifier = Modifier
                        .weight(1F)
                        .heightIn(min = buttonMinHeight)
                ) {
                    Text(
                        text = stringResource(id = LocaleR.string.cancel),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Light
                    )
                }
            }
        }
    ) {
        Text(
            text = stringResource(id = LocaleR.string.unsafe_and_untrusted),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            ),
            modifier = Modifier
                .padding(10.dp)
        )

        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
                .padding(horizontal = 10.dp)
                .verticalScroll(rememberScrollState())
        )

        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.extraSmall)
                .noIndicationClickable {
                    checkboxState = !checkboxState
                }
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(vertical = 5.dp)
                    .padding(horizontal = 10.dp)
            ) {
                val mediumEmphasis = MaterialTheme.colorScheme.onSurface.onMediumEmphasis()

                CustomCheckbox(
                    checked = checkboxState,
                    onCheckedChange = { checkboxState = it },
                    colors = CheckboxDefaults.colors().copy(
                        uncheckedBorderColor = mediumEmphasis
                    ),
                    modifier = Modifier
                        .size(14.dp)
                )

                Text(
                    text = stringResource(id = LocaleR.string.disable_warning),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = mediumEmphasis
                    )
                )
            }
        }
    }
}

@Preview
@Composable
private fun UnsafeInstallAlertDialogPreview() {
    FlixclusiveTheme {
        Surface {
            UnsafeInstallAlertDialog(
                quantity = 1,
                formattedName = "CineFlix",
                warnOnInstall = false,
                onConfirm = {},
                onDismiss = {}
            )
        }
    }
}