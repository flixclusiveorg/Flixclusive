package com.flixclusive.feature.mobile.library.common.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.components.dialog.CommonAlertDialog
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun BaseLibraryModificationDialog(
    label: String,
    name: String,
    description: String?,
    confirmLabel: String,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val labelStyle = MaterialTheme.typography.labelLarge
        .copy(color = LocalContentColor.current.copy(0.6f))
        .asAdaptiveTextStyle(increaseBy = 2.sp)

    val textFieldStyle = MaterialTheme.typography.bodyMedium.asAdaptiveTextStyle(increaseBy = 2.sp)

    val buttonMinHeight = getAdaptiveDp(50.dp)
    val buttonShape = MaterialTheme.shapes.medium

    CommonAlertDialog(
        onDismiss = onCancel,
        action = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .padding(bottom = 10.dp),
            ) {
                TextButton(
                    onClick = onConfirm,
                    shape = buttonShape,
                    modifier = Modifier
                        .weight(1F)
                        .heightIn(min = buttonMinHeight),
                ) {
                    Text(
                        text = confirmLabel,
                        style = labelStyle,
                        color = LocalContentColor.current,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 2.dp),
                    )
                }

                Button(
                    onClick = onCancel,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    shape = buttonShape,
                    modifier = Modifier
                        .weight(1F)
                        .heightIn(min = buttonMinHeight),
                ) {
                    Text(
                        text = stringResource(LocaleR.string.cancel),
                        style = labelStyle,
                        color = LocalContentColor.current,
                        fontWeight = FontWeight.Light,
                    )
                }
            }
        },
        content = {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge.asAdaptiveTextStyle(increaseBy = 2.sp),
                    modifier = Modifier.padding(bottom = 10.dp),
                )

                Text(
                    text = stringResource(LocaleR.string.name),
                    style = labelStyle,
                    modifier = Modifier.padding(bottom = 4.dp),
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = textFieldStyle,
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(LocaleR.string.description),
                    style = labelStyle,
                    modifier = Modifier.padding(bottom = 4.dp),
                )

                OutlinedTextField(
                    value = description ?: "",
                    onValueChange = onDescriptionChange,
                    textStyle = textFieldStyle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = getAdaptiveDp(100.dp)),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    shape = MaterialTheme.shapes.medium,
                )
            }
        },
    )
}
