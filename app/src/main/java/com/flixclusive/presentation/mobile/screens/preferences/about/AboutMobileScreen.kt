package com.flixclusive.presentation.mobile.screens.preferences.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
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
import com.flixclusive.BuildConfig
import com.flixclusive.R
import com.flixclusive.common.Constants.GITHUB_REPOSITORY
import com.flixclusive.presentation.mobile.screens.preferences.PreferencesNavGraph
import com.flixclusive.presentation.mobile.screens.preferences.common.TopBarWithNavigationIcon
import com.flixclusive.presentation.mobile.utils.ComposeMobileUtils
import com.flixclusive.presentation.utils.FormatterUtils.toTitleCase
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@PreferencesNavGraph
@Destination
@Composable
fun AboutMobileScreen(
    navigator: DestinationsNavigator,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .verticalScroll(rememberScrollState()),
    ) {
        TopBarWithNavigationIcon(
            headerTitle = stringResource(id = R.string.about),
            onNavigationIconClick = navigator::navigateUp
        )

        AboutScreenHeader()

        AboutScreenContent()
    }
}

@Composable
private fun AboutScreenHeader() {
    Column(
        verticalArrangement = Arrangement.spacedBy(
            space = 12.dp,
            alignment = Alignment.CenterVertically
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 25.dp)
    ) {
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher_foreground),
            contentDescription = "Flixclusive Icon",
            modifier = Modifier
                .scale(2F)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(
                space = 6.dp,
                alignment = Alignment.CenterVertically
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.app_name).uppercase(),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 16.sp,
                    //letterSpacing = TextUnit(1.05F, TextUnitType.Sp)
                ),
            )

            Surface(
                contentColor = ComposeMobileUtils.colorOnMediumEmphasisMobile()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        space = 10.dp,
                        alignment = Alignment.CenterHorizontally
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = BuildConfig.VERSION_NAME,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 13.sp
                        ),
                    )

                    Spacer(
                        modifier = Modifier
                            .height(2.dp)
                            .width(10.dp)
                            .background(
                                LocalContentColor.current,
                                MaterialTheme.shapes.large,
                            )
                    )

                    Text(
                        text = BuildConfig.BUILD_TYPE.toTitleCase(),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 13.sp
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutScreenContent() {
    val uriHandler = LocalUriHandler.current

    val fontSize = 13.sp
    val featureTitles = listOf(
        "Extensive Library",
        "Search and Discover",
        "Personalized Recommendations",
        "Continue Watching",
        "High-Quality Streaming",
        "Subtitle Selections"
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
                        append("A modern streaming service app that provides users with a convenient way to play and watch the latest movies and TV shows available on the internet. With a user-friendly interface and a vast collection of content, it aims to deliver an exceptional streaming experience to its users.\n\n")
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
                        append("App Features:\n")
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
                uriHandler.openUri(GITHUB_REPOSITORY)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = ComposeMobileUtils.colorOnMediumEmphasisMobile(Color.White)
            ),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
        ) {
            Text(
                text = stringResource(R.string.github_repository),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 16.sp
                ),
                fontWeight = FontWeight.Normal
            )
        }
    }
}