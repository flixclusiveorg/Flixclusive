package com.flixclusive.feature.mobile.provider.test.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.DummyDataForPreview.getDummyProviderData
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.gradle.entities.ProviderData
import com.flixclusive.core.util.R as UtilR

private val ButtonHeight = 40.dp
private val CardShape = RoundedCornerShape(8.dp)
private val ContentPadding = PaddingValues(
    vertical = 10.dp,
    horizontal = 16.dp
)

@Composable
internal fun TestResultCard(
    provider: ProviderData,
    checksPassed: Int,
    totalChecks: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val maxContentHeight = when(isExpanded) {
        true -> Dp.Unspecified
        false -> 0.dp
    }

    Box(contentAlignment = Alignment.TopCenter) {
        TestResultCardContent(
            modifier = Modifier
                .animateContentSize(
                    animationSpec = tween(durationMillis = 100)
                )
                .heightIn(
                    min = ButtonHeight * 2,
                    max = maxContentHeight
                )
        )

        TestResultCardHeader(
            provider = getDummyProviderData(),
            checksPassed = 1,
            totalChecks = 3,
            isExpanded = isExpanded,
            onToggle = onToggle
        )
    }
}

@Composable
private fun TestResultCardHeader(
    provider: ProviderData,
    checksPassed: Int,
    totalChecks: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val coolGradient = Brush.horizontalGradient(
        0F to MaterialTheme.colorScheme.tertiary,
        0.7F to MaterialTheme.colorScheme.primary
    )

    Button(
        onClick = onToggle,
        enabled = true,
        shape = CardShape,
        colors = ButtonDefaults.buttonColors(
            contentColor = LocalContentColor.current,
            containerColor = Color.Transparent
        ),
        contentPadding = ContentPadding,
        modifier = Modifier
            .height(ButtonHeight)
            .shadow(
                elevation = 1.dp,
                shape = CardShape,
                clip = true,
                spotColor = Color.Transparent
            )
            .drawBehind {
                drawRect(surfaceColor)
                drawRect(coolGradient, alpha = 0.15F)
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "$checksPassed/$totalChecks",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = LocalContentColor.current.onMediumEmphasis()
                ),
            )

            Text(
                modifier = Modifier.weight(1F),
                text = provider.name ,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
            )

            AnimatedContent(
                targetState = isExpanded,
                label = ""
            ) {
                val icon = if (it) {
                    Icons.Rounded.KeyboardArrowUp
                } else Icons.Rounded.KeyboardArrowDown

                Icon(
                    imageVector = icon,
                    contentDescription = stringResource(id = UtilR.string.expand_card_icon_content_desc),
                    tint = LocalContentColor.current.onMediumEmphasis(0.8F)
                )
            }
        }
    }
}

@Composable
private fun TestResultCardContent(
    modifier: Modifier = Modifier
) {
    val extraCutOutPadding = ButtonHeight.times(0.15F)

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = ButtonHeight.minus(extraCutOutPadding)),
        shape = CardShape,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ContentPadding)
                .padding(top = extraCutOutPadding + 4.dp)
        ) {
            Text(
                text = "Movie used:",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    color = LocalContentColor.current.onMediumEmphasis()
                ),
            )
        }
    }
}

@Preview
@Composable
private fun TestResultCardPreview() {
    val providers = List(5) { getDummyProviderData() }
    val isExpandedMap = remember {
        List(providers.size) { index: Int -> index to false }
            .toMutableStateMap()
    }

    FlixclusiveTheme {
        Surface {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(providers) { i, data ->
                    TestResultCard(
                        provider = data,
                        checksPassed = 1,
                        totalChecks = 3,
                        isExpanded = isExpandedMap[i] ?: true,
                        onToggle = {
                            isExpandedMap[i] = !(isExpandedMap[i] ?: true)
                        }
                    )
                }
            }
        }
    }
}