package com.flixclusive.feature.mobile.user.pin.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flixclusive.core.database.entity.user.MAX_USER_PIN_LENGTH
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.material3.topbar.CommonTopBar
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PinSetupScreenCompactLandscape(
    pin: MutableState<String>,
    isTyping: MutableState<Boolean>,
    hasErrors: MutableState<Boolean>,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
    title: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        CommonTopBar(
            title = "",
            onNavigate = onBack,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth(0.8F)
                    .align(Alignment.Center)
                    .scale(0.85F),
        ) {
            Column(
                modifier = Modifier.weight(0.5F),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement =
                    Arrangement.spacedBy(
                        space = 25.dp,
                        alignment = Alignment.CenterVertically,
                    ),
            ) {
                title()

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    repeat(MAX_USER_PIN_LENGTH) {
                        PinPlaceholder(
                            showPin = it == pin.value.length - 1 && isTyping.value,
                            hasErrors = hasErrors.value,
                            char = pin.value.getOrNull(it),
                        )
                    }
                }
            }

            val pinPadding = getAdaptiveDp(20.dp)

            FlowRow(
                modifier = Modifier.weight(0.5F),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        space = pinPadding,
                        alignment = Alignment.CenterHorizontally,
                    ),
                verticalArrangement = Arrangement.spacedBy(pinPadding),
                maxItemsInEachRow = 3,
            ) {
                repeat(MAX_NUMBER_LENGTH + 2) {
                    when (val digit = it + 1) {
                        10 -> {
                            PinButton(
                                enabled = pin.value.isNotEmpty(),
                                noEmphasis = true,
                                onClick = {
                                    if (pin.value.isNotEmpty()) {
                                        isTyping.value = false
                                        pin.value = pin.value.dropLast(1)
                                    }
                                },
                            ) {
                                AdaptiveIcon(
                                    painter = painterResource(UiCommonR.drawable.backspace_filled),
                                    contentDescription = stringResource(LocaleR.string.backspace_content_desc),
                                )
                            }
                        }

                        MAX_NUMBER_LENGTH + 2 -> {
                            PinButton(
                                enabled = pin.value.length == MAX_USER_PIN_LENGTH,
                                noEmphasis = true,
                                onClick = {
                                    if (pin.value.length == MAX_USER_PIN_LENGTH) {
                                        onConfirm()
                                    }
                                },
                            ) {
                                Text(
                                    text = stringResource(LocaleR.string.ok),
                                    fontWeight = FontWeight.Black,
                                    style = MaterialTheme.typography.titleMedium.asAdaptiveTextStyle(),
                                )
                            }
                        }

                        else -> {
                            val coercedDigit = digit.coerceAtMost(MAX_NUMBER_LENGTH)
                            PinButton(
                                digit = coercedDigit % MAX_NUMBER_LENGTH,
                                onClick = {
                                    if (pin.value.length < MAX_USER_PIN_LENGTH) {
                                        isTyping.value = true
                                        hasErrors.value = false
                                        pin.value += "${coercedDigit % MAX_NUMBER_LENGTH}"
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
