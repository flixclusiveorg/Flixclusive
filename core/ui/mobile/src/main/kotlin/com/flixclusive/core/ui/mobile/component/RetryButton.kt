package com.flixclusive.core.ui.mobile.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.CustomClipboardManager.Companion.rememberClipboardManager
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

val LARGE_ERROR = 480.dp
val SMALL_ERROR = 110.dp

private const val MAX_STACK_TRACE_COMPONENT_HEIGHT = 200

@Composable
fun RetryButton(
    modifier: Modifier = Modifier,
    shouldShowError: Boolean = false,
    error: String? = null,
    onRetry: () -> Unit,
) {
    AnimatedVisibility(
        visible = shouldShowError,
        enter = scaleIn(),
        exit = scaleOut()
    ) {
        val defaultLabel = stringResource(LocaleR.string.something_went_wrong)

        var isErrorLogsShown by remember { mutableStateOf(false) }
        val isStackTrace = remember(error) {
            error != null && isPossibleStackTrace(error)
        }

        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                EmptyDataMessage(
                    modifier = Modifier.padding(horizontal = 15.dp),
                    title = stringResource(LocaleR.string.an_error_occurred),
                    description = if (isStackTrace) defaultLabel else error ?: defaultLabel,
                    icon = {
                        Icon(
                            painter = painterResource(UiCommonR.drawable.round_error_outline_24),
                            contentDescription = stringResource(LocaleR.string.error_icon_content_desc),
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.error.onMediumEmphasis()
                        )
                    }
                )

                if (isStackTrace) {
                    val contentColor = MaterialTheme.colorScheme.onSurface.onMediumEmphasis(0.8F)

                    Column(
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AnimatedContent(
                            targetState = isErrorLogsShown,
                            label = ""
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.extraSmall)
                                    .clickable { isErrorLogsShown = !isErrorLogsShown },
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(3.dp)
                                        .padding(horizontal = 5.dp),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (it) stringResource(LocaleR.string.hide_error_logs) else stringResource(LocaleR.string.show_error_logs),
                                        style = LocalTextStyle.current.copy(
                                            color = contentColor,
                                            fontSize = 12.sp
                                        )
                                    )

                                    Icon(
                                        painter = if (it) painterResource(UiCommonR.drawable.up_arrow) else painterResource(UiCommonR.drawable.down_arrow),
                                        contentDescription = stringResource(
                                            if (isErrorLogsShown) LocaleR.string.overview_collapse else LocaleR.string.overview_expand,
                                        ),
                                        tint = contentColor,
                                        modifier = Modifier
                                            .size(10.dp)
                                    )
                                }
                            }
                        }

                        TextFieldStackTrace(
                            stackTrace = error!!,
                            modifier = Modifier
                                .animateContentSize(tween())
                                .fillMaxWidth(0.95F)
                                .height(if (isErrorLogsShown) MAX_STACK_TRACE_COMPONENT_HEIGHT.dp else 0.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onRetry,
                    shape = ShapeDefaults.ExtraSmall
                ) {
                    Text(
                        text = stringResource(LocaleR.string.retry),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun TextFieldStackTrace(
    modifier: Modifier = Modifier,
    stackTrace: String,
) {
    val clipboardManager = rememberClipboardManager()

    val containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)

    Box(
        modifier = modifier
    ) {
        TextField(
            value = stackTrace,
            onValueChange = {},
            modifier = Modifier
                .fillMaxSize(),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Normal
            ),
            shape = MaterialTheme.shapes.extraSmall,
            readOnly = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = containerColor,
                unfocusedContainerColor = containerColor,
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
            )
        )

        Surface(
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
            shape = MaterialTheme.shapes.extraSmall,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .clickable {
                        clipboardManager.setText(stackTrace)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(UiCommonR.drawable.round_content_copy_24),
                    contentDescription = stringResource(LocaleR.string.copy_stack_trace_content_desc),
                    tint = LocalContentColor.current.onMediumEmphasis()
                )
            }
        }
    }
}

private fun isPossibleStackTrace(input: String): Boolean {
    val stackTracePatterns = listOf(
        "at ",              // Common line starter
        "Caused by: ",      // Exception cause indicator
        "java.",            // Java packages
        "kotlin.",          // Kotlin packages
        "android.",         // Android packages
    )

    return stackTracePatterns.fastAny { input.contains(it) }
}

@Preview
@Composable
private fun RetryButtonPreview() {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
        ) {
            RetryButton(
                shouldShowError = true,
                error = """
                    lang.NullPointerExceptione
                """.trimIndent()
            ) {

            }
        }
    }
}