package com.flixclusive.feature.mobile.settings.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.network.util.okhttp.UserAgentManager
import com.flixclusive.core.ui.common.util.createTextFieldValue
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.util.R as UtilR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditUserAgentDialog(
    defaultUserAgent: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val buttonMinHeight = 50.dp
    val buttonShape = MaterialTheme.shapes.medium
    val buttonShapeRoundnessPercentage = 10

    val focusRequester = remember { FocusRequester() }

    var isError by remember { mutableStateOf(false) }
    var textFieldValue by remember {
        mutableStateOf(defaultUserAgent.createTextFieldValue())
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(buttonShapeRoundnessPercentage),
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(10.dp)
                        .weight(1F, fill = false)
                ) {
                    Text(
                        text = stringResource(id = UtilR.string.default_user_agent),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(10.dp)
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        TextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 15.dp)
                                .focusRequester(focusRequester),
                            value = textFieldValue,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            shape = MaterialTheme.shapes.extraSmall,
                            isError = isError,
                            onValueChange = {
                                isError = false
                                textFieldValue = it
                            },
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    val newUserAgent = textFieldValue.text
                                    if (newUserAgent.isBlank()) {
                                        isError = true
                                        return@KeyboardActions
                                    }

                                    onConfirm(newUserAgent)
                                }
                            ),
                            placeholder = {
                                Text(
                                    text = "Paste addon url",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LocalContentColor.current.onMediumEmphasis(),
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1
                                )
                            },
                            trailingIcon = {
                                AnimatedVisibility(
                                    visible = textFieldValue.text.isNotEmpty(),
                                    enter = scaleIn(),
                                    exit = scaleOut(),
                                ) {
                                    IconButton(
                                        onClick = {
                                            textFieldValue = "".createTextFieldValue()
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(UiCommonR.drawable.round_close_24),
                                            contentDescription = stringResource(UtilR.string.clear_text_button)
                                        )
                                    }
                                }
                            },
                        )

                        ElevatedButton(
                            onClick = {
                                isError = false
                                textFieldValue = UserAgentManager
                                    .getRandomUserAgent()
                                    .createTextFieldValue()
                            },
                            shape = MaterialTheme.shapes.extraSmall,
                            modifier = Modifier
                                .heightIn(min = buttonMinHeight)
                                .widthIn(min = 150.dp)
                        ) {
                            Text(
                                text = stringResource(id = UtilR.string.randomize),
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .padding(bottom = 10.dp)
                ) {
                    Button(
                        onClick = {
                            val newUserAgent = textFieldValue.text
                            if (newUserAgent.isBlank()) {
                                isError = true
                                return@Button
                            }

                            onConfirm(newUserAgent)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
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
                            text = stringResource(id = UtilR.string.save),
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
                            bottomEnd = CornerSize((buttonShapeRoundnessPercentage  *2).dp),
                        ),
                        modifier = Modifier
                            .weight(1F)
                            .heightIn(min = buttonMinHeight)
                    ) {
                        Text(
                            text = stringResource(id = UtilR.string.cancel),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Light
                        )
                    }
                }
            }
        }
    }
}