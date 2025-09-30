package com.flixclusive.feature.mobile.user.add.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun NavigationButtons(
    canSkip: Boolean,
    disableNextButton: Boolean,
    isFinalStep: Boolean,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val adaptiveHeight = Modifier.height(getAdaptiveDp(45.dp, 10.dp))
    val weight by animateFloatAsState(
        targetValue = if (canSkip) 0.5F else 1F,
        label = "Modifier.weight"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AnimatedVisibility(
            visible = canSkip,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.weight(0.5F)
        ) {
            TextButton(
                onClick = onNext,
                shape = MaterialTheme.shapes.large,
                modifier = adaptiveHeight.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(LocaleR.string.skip),
                    style = MaterialTheme.typography.labelLarge.asAdaptiveTextStyle(),
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        FilledTonalButton(
            onClick = onNext,
            enabled = !disableNextButton,
            shape = MaterialTheme.shapes.large,
            modifier = adaptiveHeight
                .weight(weight)
        ) {
            AnimatedContent(
                targetState = isFinalStep,
                label = "FilledTonalButton.AnimatedContent"
            ) { state ->
                Text(
                    text = if (state) stringResource(LocaleR.string.finish) else stringResource(LocaleR.string.next),
                    style = MaterialTheme.typography.labelLarge.asAdaptiveTextStyle(increaseBy = 6.sp),
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}
