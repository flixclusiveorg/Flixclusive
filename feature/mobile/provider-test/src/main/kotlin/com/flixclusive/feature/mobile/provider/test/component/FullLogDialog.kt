package com.flixclusive.feature.mobile.provider.test.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.CustomClipboardManager.Companion.rememberClipboardManager
import com.flixclusive.core.ui.common.util.DummyDataForPreview.getDummyProviderData
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.util.common.ui.UiText
import com.flixclusive.domain.provider.test.ProviderTestCaseOutput
import com.flixclusive.domain.provider.test.TestStatus
import com.flixclusive.gradle.entities.ProviderData
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.util.R as UtilR

private val FullLogDialogShape = RoundedCornerShape(10)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FullLogDialog(
    testCaseOutput: ProviderTestCaseOutput,
    provider: ProviderData,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = rememberClipboardManager()

    val labels = getFullLogOtherLabels(
        provider = provider,
        testCaseOutput = testCaseOutput
    )

    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = FullLogDialogShape,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(180.dp)
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(bottom = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(space = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    with(testCaseOutput.status) {
                        Icon(
                            painter = painterResource(id = iconId),
                            tint = Color(color),
                            contentDescription = toString(),
                            modifier = Modifier.size(23.dp)
                        )
                    }

                    Text(
                        text = testCaseOutput.name.asString(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.weight(1F)
                    )

                    IconButton(
                        onClick = {
                            val toCopy = formatFullLog(
                                testName = testCaseOutput.name.asString(context),
                                otherLabels = labels,
                                fullLog = testCaseOutput.fullLog?.asString(context)
                                    ?: context.getString(UtilR.string.no_full_log)
                            )

                            clipboardManager.setText(toCopy)
                        },
                        modifier = Modifier.size(23.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = UiCommonR.drawable.round_content_copy_24),
                            tint = LocalContentColor.current.onMediumEmphasis(),
                            contentDescription = stringResource(id = UtilR.string.copy_full_logs_button),
                            modifier = Modifier.size(23.dp)
                        )
                    }
                }

                labels.forEach {
                    Text(text = it)
                }

                HorizontalDivider(
                    color = LocalContentColor.current.onMediumEmphasis(),
                    modifier = Modifier
                        .padding(vertical = 5.dp),
                )

                TextField(
                    value = testCaseOutput.fullLog?.asString()
                        ?: stringResource(id = UtilR.string.no_full_log),
                    onValueChange = {},
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,

                        ),
                    shape = MaterialTheme.shapes.extraSmall,
                    readOnly = true,
                    colors = TextFieldDefaults.colors(
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                    )
                )

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = MaterialTheme.shapes.extraSmall.copy(
                        bottomStart = CornerSize(20.dp),
                        bottomEnd = CornerSize(20.dp),
                    ),
                    modifier = Modifier
                        .height(60.dp)
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                ) {
                    Text(
                        text = stringResource(id = UtilR.string.close_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Light
                    )
                }
            }
        }
    }
}

@Composable
internal fun labelValueStringBuilder(
    @StringRes label: Int,
    value: String
): AnnotatedString {
    val context = LocalContext.current

    val defaultStyle = MaterialTheme.typography.bodyMedium.copy(
        fontSize = 13.sp,
    ).toSpanStyle()

    val mediumEmphasisColor = LocalContentColor.current.onMediumEmphasis()

    return buildAnnotatedString {
        withStyle(
            style = ParagraphStyle(lineHeight = 20.sp)
        ) {
            withStyle(
                style = defaultStyle.copy(
                    fontWeight = FontWeight.Bold,
                    color = mediumEmphasisColor
                )
            ) {
                append(context.getString(label) + " ")
            }

            withStyle(
                style = defaultStyle.copy(
                    fontWeight = FontWeight.Normal,
                )
            ) {
                append(value)
            }
        }

    }
}

@Composable
internal fun getFullLogOtherLabels(
    provider: ProviderData,
    testCaseOutput: ProviderTestCaseOutput
): List<AnnotatedString> {
    val context = LocalContext.current

    return listOf(
        labelValueStringBuilder(
            label = UtilR.string.full_log_provider_used_format,
            value = provider.name
        ),
        labelValueStringBuilder(
            label = UtilR.string.full_log_version_used_format,
            value = "${provider.versionName} (${provider.versionCode})"
        ),
        labelValueStringBuilder(
            label = UtilR.string.full_log_short_log_used_format,
            value = testCaseOutput.shortLog?.asString(context)
                ?: stringResource(UtilR.string.no_short_log)
        ),
        labelValueStringBuilder(
            label = UtilR.string.full_log_time_taken_format,
            value = (testCaseOutput.timeTaken ?: 0.seconds).toString(unit = DurationUnit.SECONDS)
        )
    )
}

internal fun formatFullLog(
    testName: String,
    otherLabels: List<AnnotatedString>,
    fullLog: String
): String {
    return """
Test case name: $testName
        
-- [Other details] --
${otherLabels.joinToString("\n")}
        
-- [Full log] --
$fullLog
    """.trimIndent()
}

@Preview
@Composable
private fun FullLogDialogPreview() {
    val testCaseOutput = ProviderTestCaseOutput(
        status = TestStatus.FAILURE,
        name = UiText.StringValue("Get film details"),
        timeTaken = 10.seconds,
        fullLog = UiText.StringValue(
            "Exception in thread \"main\" java.lang.NullPointerException: Cannot invoke \"com.example.myapp.MyClass.getValue()\" because \"myObject\" is null\n" +
                    "    at com.example.myapp.MyApplication.performAction(MyApplication.java:35)\n" +
                    "    at com.example.myapp.MyApplication.main(MyApplication.java:15)\n" +
                    "Caused by: java.lang.NullPointerException\n" +
                    "    at com.example.myapp.MyClass.getValue(MyClass.java:22)\n" +
                    "    at com.example.myapp.MyApplication.performAction(MyApplication.java:32)\n" +
                    "    ... 1 more"
        ),
        shortLog = UiText.StringValue("Failed to fetch film details for The Godfather [tt0068646]")
    )

    val provider = getDummyProviderData()

    FlixclusiveTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            FullLogDialog(testCaseOutput = testCaseOutput, provider = provider) {

            }
        }
    }
}