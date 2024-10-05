package com.flixclusive.core.ui.mobile.component

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
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.locale.R as LocaleR

@Composable
private fun getDefaultTitleStyle()
    = MaterialTheme.typography.titleMedium.copy(
        letterSpacing = 3.8.sp,
        fontWeight = FontWeight.Black
    )

@Composable
private fun getDefaultDescriptionStyle()
    = MaterialTheme.typography.bodyMedium.copy(
        letterSpacing = 1.sp,
        fontWeight = FontWeight.Normal,
        color = LocalContentColor.current.onMediumEmphasis(0.8F)
    )

@Composable
fun EmptyDataMessage(
    modifier: Modifier = Modifier,
    title: String = stringResource(id = LocaleR.string.empty_data_default_label),
    description: String = stringResource(id = LocaleR.string.empty_data_default_sub_label),
    titleStyle: TextStyle = getDefaultTitleStyle(),
    descriptionStyle: TextStyle = getDefaultDescriptionStyle(),
    alignment: Alignment = Alignment.Center,
    icon: @Composable (() -> Unit)? = null
) {
    val titleUppercase = remember(title) {
        title.uppercase()
    }

    Box(
        modifier = Modifier
            .then(modifier),
        contentAlignment = alignment
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (icon != null) {
                icon()
            } else {
                Text(
                    text = "\uD83E\uDD97",
                    style = MaterialTheme.typography.displayMedium.copy(
                        shadow = Shadow(
                            offset = Offset(4F, 5F)
                        )
                    )
                )
            }


            SelectionContainer {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            ParagraphStyle(
                                lineHeight = 20.sp,
                                textAlign = TextAlign.Center,
                            )
                        ) {
                            withStyle(
                                style = titleStyle.toSpanStyle()
                            ) {
                                append(titleUppercase)
                            }

                            append("\n")

                            withStyle(
                                style = descriptionStyle.toSpanStyle()
                            ) {
                                append(description)
                            }
                        }
                    },
                    style = titleStyle,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Preview
@Composable
private fun EmptyDataMessagePreview() {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            EmptyDataMessage {
                Text(
                    text = "\uD83E\uDD97",
                    style = MaterialTheme.typography.displayMedium.copy(
                        shadow = Shadow(
                            offset = Offset(4F, 5F)
                        )
                    )
                )
            }
        }
    }
}