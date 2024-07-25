package com.flixclusive.feature.mobile.provider.test.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.DummyDataForPreview.getDummyProviderData
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.gradle.entities.ProviderData
import com.flixclusive.core.util.R as UtilR

private val HeaderLabelSpacing = 50.dp

@Composable
internal fun TestScreenHeader(
    modifier: Modifier = Modifier,
    providers: List<ProviderData>,
    currentTesting: Int,
) {
    val currentProviderToTest = remember(currentTesting) {
        providers.getOrNull(currentTesting)
    }

    Box(
        modifier = modifier
            .heightIn(min = 280.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedContent(
            targetState = currentProviderToTest != null,
            label = "",
        ) {
            when (it) {
                true -> {
                    HeaderLabels(
                        modifier = Modifier
                            .padding(top = 100.dp, bottom = HeaderLabelSpacing),
                        currentProviderToTest = currentProviderToTest!!
                    )
                }
                false -> TODO()
            }
        }


    }
}

@Composable
private fun HeaderLabels(
    modifier: Modifier = Modifier,
    currentProviderToTest: ProviderData,
) {
    val context = LocalContext.current
    val testingLabel = remember {
        context.getString(UtilR.string.currently_testing).uppercase()
    }
    val testingStageLabel = remember {
        context.getString(UtilR.string.stage).uppercase()
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(HeaderLabelSpacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = testingLabel,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 1.sp,
                    color = LocalContentColor.current.onMediumEmphasis()
                ),
            )

            Text(
                text = currentProviderToTest.name,
                style = MaterialTheme.typography.headlineMedium.copy(
                    textAlign = TextAlign.Center
                ),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = testingStageLabel,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = LocalContentColor.current.onMediumEmphasis(0.8F)
                ),
            )

            /*TODO("Make a dynamic sealed/enum class for stages")*/
            Text(
                text = "Done" ,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                ),
            )
            /*TODO("Add a loading progress component here")*/
        }
    }
}

@Preview
@Composable
private fun ScreenHeaderPreview() {
    FlixclusiveTheme {
        Surface {
            TestScreenHeader(
                providers = listOf(getDummyProviderData().copy(name = "CINEFLIXHAHAHAHAHAHAHAHHAHAHHAHAHAHAH")),
                currentTesting = 0
            )
        }
    }
}