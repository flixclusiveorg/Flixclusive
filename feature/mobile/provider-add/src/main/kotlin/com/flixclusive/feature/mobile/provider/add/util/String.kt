package com.flixclusive.feature.mobile.provider.add.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.util.fastMap
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveTextStyle
import com.flixclusive.core.ui.common.util.adaptive.TextStyleMode
import com.flixclusive.model.provider.Repository
import com.flixclusive.core.strings.R as LocaleR

internal fun String.toUpdaterJsonUrl(): String {
    val buildUrlPath = substringBeforeLast("/")
    return "$buildUrlPath/updater.json"
}

@Stable
@Composable
internal fun Context.getErrorLog(failedToInitializeRepositories: List<Repository>): AnnotatedString {
    val textStyle = getAdaptiveTextStyle(mode = TextStyleMode.Normal)

    return buildAnnotatedString {
        withStyle(ParagraphStyle(textAlign = TextAlign.Start)) {
            withStyle(textStyle.toSpanStyle()) {
                append(getString(LocaleR.string.failed_to_load_the_ff))
            }
            append("\n")
            withStyle(ParagraphStyle(lineHeight = 1.5.em)) {
                withStyle(
                    textStyle
                        .copy(fontWeight = FontWeight.Medium)
                        .toSpanStyle(),
                ) {
                    val repositories =
                        failedToInitializeRepositories
                            .fastMap { "${it.owner}/${it.name}" }
                            .joinToString("\n")

                    append(repositories)
                }
            }
        }
    }
}
