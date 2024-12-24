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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveTextStyle
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.core.ui.common.util.adaptive.TextStyleMode
import com.flixclusive.core.ui.common.util.adaptive.TypographyStyle
import com.flixclusive.feature.mobile.user.add.util.StateHoistingUtil.LocalUserToAdd
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun NavigationButtons(
    modifier: Modifier = Modifier,
    canSkip: Boolean,
    isFinalStep: Boolean,
    onNext: () -> Unit
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
                    style = getAdaptiveTextStyle(
                        style = TypographyStyle.Label,
                        mode = TextStyleMode.NonEmphasized,
                    ).copy(
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        FilledTonalButton(
            onClick = onNext,
            enabled = LocalUserToAdd.current.value.name.isNotEmpty(),
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
                    style = getAdaptiveTextStyle(
                        style = TypographyStyle.Label,
                        mode = TextStyleMode.Emphasized,
                        increaseBy = 6.sp
                    )
                )
            }
        }
    }
}