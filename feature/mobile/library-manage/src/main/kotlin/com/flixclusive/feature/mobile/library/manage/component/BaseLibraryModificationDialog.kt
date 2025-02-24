package com.flixclusive.feature.mobile.library.manage.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.ui.common.dialog.ALERT_DIALOG_CORNER_SIZE
import com.flixclusive.core.ui.common.dialog.CustomBaseAlertDialog
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveTextStyle
import com.flixclusive.core.ui.common.util.adaptive.TextStyleMode
import com.flixclusive.core.ui.common.util.adaptive.TypographyStyle
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.locale.R as LocaleR

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
    val labelStyle =
        getAdaptiveTextStyle(
            mode = TextStyleMode.Emphasized,
            style = TypographyStyle.Label,
            increaseBy = 2.sp,
        ).copy(color = LocalContentColor.current.onMediumEmphasis())

    val buttonMinHeight = 50.dp
    val baseShape = MaterialTheme.shapes.medium
    val cornerSize = CornerSize((ALERT_DIALOG_CORNER_SIZE * 2).dp)
    val buttonShape =
        baseShape.copy(
            bottomStart = cornerSize,
            bottomEnd = baseShape.bottomEnd,
        )

    CustomBaseAlertDialog(
        onDismiss = onCancel,
        action = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier =
                    Modifier
                        .padding(horizontal = 10.dp)
                        .padding(bottom = 10.dp),
            ) {
                TextButton(
                    onClick = onConfirm,
                    shape = buttonShape,
                    modifier =
                        Modifier
                            .weight(1F)
                            .heightIn(min = buttonMinHeight),
                ) {
                    Text(
                        text = confirmLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier =
                            Modifier
                                .padding(end = 2.dp),
                    )
                }

                Button(
                    onClick = onCancel,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.Black,
                        ),
                    shape =
                        buttonShape.copy(
                            bottomStart = baseShape.bottomStart,
                            bottomEnd = cornerSize,
                        ),
                    modifier =
                        Modifier
                            .weight(1F)
                            .heightIn(min = buttonMinHeight),
                ) {
                    Text(
                        text = stringResource(LocaleR.string.cancel),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Light,
                    )
                }
            }
        },
        content = {
            Column(
                modifier =
                    Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
            ) {
                Text(
                    text = label,
                    style = getAdaptiveTextStyle(
                        mode = TextStyleMode.Emphasized,
                        style = TypographyStyle.Title,
                        increaseBy = 2.sp,
                    ),
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
                    textStyle = MaterialTheme.typography.bodyMedium,
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
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                    maxLines = 4,
                    keyboardOptions =
                        KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Done,
                        ),
                    shape = MaterialTheme.shapes.medium,
                )
            }
        },
    )
}
