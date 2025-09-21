package com.flixclusive.core.presentation.mobile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.strings.R as LocaleR

@Composable
fun EmptyDataMessage(
    modifier: Modifier = Modifier,
    title: String = stringResource(id = LocaleR.string.empty_data_default_label),
    description: String = stringResource(id = LocaleR.string.empty_data_default_sub_label),
    emojiHeader: String = "\uD83E\uDD97",
    titleStyle: TextStyle = defaultTitleStyle,
    descriptionStyle: TextStyle = defaultDescriptionStyle,
    alignment: Alignment = Alignment.Center,
    icon: @Composable (() -> Unit)? = null,
) {
    val titleUppercase = remember(title) {
        title.uppercase()
    }

    Box(
        modifier = Modifier
            .then(modifier),
        contentAlignment = alignment,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (icon != null) {
                icon()
            } else {
                Text(
                    text = emojiHeader,
                    style = MaterialTheme.typography.displayMedium.copy(
                        shadow = Shadow(
                            offset = Offset(4F, 5F),
                        ),
                    ),
                )
            }

            SelectionContainer {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            ParagraphStyle(
                                lineHeight = 20.sp,
                                textAlign = TextAlign.Center,
                            ),
                        ) {
                            withStyle(
                                style = titleStyle.toSpanStyle(),
                            ) {
                                append(titleUppercase)
                            }

                            append("\n")

                            withStyle(
                                style = descriptionStyle.toSpanStyle(),
                            ) {
                                append(description)
                            }
                        }
                    },
                    style = titleStyle,
                    modifier = Modifier
                        .padding(horizontal = 16.dp),
                )
            }
        }
    }
}

private val defaultTitleStyle
    @Composable get() =
        MaterialTheme.typography.titleMedium.copy(
            letterSpacing = 3.8.sp,
            fontWeight = FontWeight.Black,
        ).asAdaptiveTextStyle()

private val defaultDescriptionStyle
    @Composable get() =
        MaterialTheme.typography.bodyMedium.copy(
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Normal,
            color = LocalContentColor.current.copy(0.8F),
        ).asAdaptiveTextStyle()

@Preview
@Composable
private fun EmptyDataMessageBasePreview() {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            EmptyDataMessage {
                Text(
                    text = "\uD83E\uDD97",
                    style = MaterialTheme.typography.displayMedium.copy(
                        shadow = Shadow(offset = Offset(4F, 5F)),
                    ),
                )
            }
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun EmptyDataMessageCompactLandscapePreview() {
    EmptyDataMessageBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun EmptyDataMessageMediumPortraitPreview() {
    EmptyDataMessageBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun EmptyDataMessageMediumLandscapePreview() {
    EmptyDataMessageBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun EmptyDataMessageExtendedPortraitPreview() {
    EmptyDataMessageBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun EmptyDataMessageExtendedLandscapePreview() {
    EmptyDataMessageBasePreview()
}
