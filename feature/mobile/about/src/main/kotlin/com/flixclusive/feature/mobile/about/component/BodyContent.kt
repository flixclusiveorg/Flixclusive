package com.flixclusive.feature.mobile.about.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.locale.UiText
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.util.common.GithubConstant.GITHUB_REPOSITORY_URL
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun BodyContent() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val fontSize = 13.sp
    val featureTitles = listOf(
        stringResource(LocaleR.string.extensive_library),
        stringResource(LocaleR.string.search_and_discover),
        stringResource(LocaleR.string.personalized_recommendations),
        stringResource(LocaleR.string.smart_watch_history),
        stringResource(LocaleR.string.high_quality_streaming),
        stringResource(LocaleR.string.subtitle_selections)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth(),
            text = buildAnnotatedString {
                withStyle(
                    style = ParagraphStyle(
                        lineBreak = LineBreak.Paragraph,
                        textAlign = TextAlign.Justify,
                        textIndent = TextIndent(firstLine = 15.sp),
                    )
                ) {
                    withStyle(style = SpanStyle(fontSize = fontSize)) {
                        append(UiText.StringResource(LocaleR.string.app_description).asString(context))
                        append("\n")
                    }
                }

                withStyle(
                    style = ParagraphStyle(
                        lineHeight = 10.sp
                    )
                ) {
                    withStyle(
                        style = SpanStyle(
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(UiText.StringResource(LocaleR.string.privacy_notice).asString(context))
                        append("\n")
                    }
                }

                withStyle(
                    style = ParagraphStyle(
                        lineBreak = LineBreak.Paragraph,
                        textAlign = TextAlign.Justify,
                        textIndent = TextIndent(firstLine = 15.sp),
                    )
                ) {
                    withStyle(style = SpanStyle(fontSize = fontSize)) {
                        append(UiText.StringResource(LocaleR.string.privacy_notice_crash_log_sender).asString(context))
                        append("\n")
                    }
                }

                withStyle(
                    style = ParagraphStyle(
                        lineHeight = 10.sp
                    )
                ) {
                    withStyle(
                        style = SpanStyle(
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(UiText.StringResource(LocaleR.string.provider_disclaimer).asString(context))
                        append("\n")
                    }
                }

                withStyle(
                    style = ParagraphStyle(
                        lineBreak = LineBreak.Paragraph,
                        textAlign = TextAlign.Justify,
                        textIndent = TextIndent(firstLine = 15.sp),
                    )
                ) {
                    withStyle(style = SpanStyle(fontSize = fontSize)) {
                        append(UiText.StringResource(LocaleR.string.disclaimer_provider_message).asString(context))
                        append("\n")
                    }
                }


                withStyle(
                    style = ParagraphStyle(
                        lineHeight = 10.sp
                    )
                ) {
                    withStyle(
                        style = SpanStyle(
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(UiText.StringResource(LocaleR.string.app_features).asString(context))
                        append("\n")
                    }
                }

                withStyle(
                    style = ParagraphStyle(
                        textIndent = TextIndent(firstLine = 15.sp),
                        lineBreak = LineBreak.Simple
                    )
                ) {
                    featureTitles.forEach {
                        withStyle(style = SpanStyle(fontSize = fontSize)) {
                            append("•\t\t")
                            append(it)
                            append("\n")
                        }
                    }
                }
            }
        )

        Button(
            onClick = {
                uriHandler.openUri(GITHUB_REPOSITORY_URL)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = Color.White.onMediumEmphasis()
            ),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
        ) {
            Text(
                text = stringResource(LocaleR.string.github_repository),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 16.sp
                ),
                fontWeight = FontWeight.Normal
            )
        }
    }
}