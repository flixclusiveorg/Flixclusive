package com.flixclusive.feature.mobile.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastFlatMap
import androidx.compose.ui.util.fastForEachIndexed
import com.flixclusive.core.ui.common.adaptive.AdaptiveIcon
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveTextStyle
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.core.ui.common.util.adaptive.TextStyleMode
import com.flixclusive.core.ui.common.util.adaptive.TypographyStyle
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.util.coroutines.AppDispatchers
import com.flixclusive.feature.mobile.settings.component.BaseTweakComponent
import com.flixclusive.feature.mobile.settings.component.ClickableComponent
import com.flixclusive.feature.mobile.settings.component.DialogComponent
import com.flixclusive.feature.mobile.settings.component.ListRadioComponent
import com.flixclusive.feature.mobile.settings.component.ListSelectComponent
import com.flixclusive.feature.mobile.settings.component.SliderComponent
import com.flixclusive.feature.mobile.settings.component.SwitchComponent
import com.flixclusive.feature.mobile.settings.component.TextFieldComponent
import com.flixclusive.feature.mobile.settings.component.TitleDescriptionHeader
import com.flixclusive.feature.mobile.settings.util.LocalSettingsSearchQuery
import com.flixclusive.feature.mobile.settings.util.getEmphasizedLabel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal val TweakPaddingHorizontal = 10.dp
private val TweakGroupSpacing = 25.dp

@Composable
internal fun TweakScaffold(
    title: String,
    description: String,
    tweaksProvider: @Composable () -> List<Tweak>,
) {
    val screenPadding = getAdaptiveDp(TweakPaddingHorizontal)

    val tweaks = tweaksProvider()
    val settingsSearchQuery = LocalSettingsSearchQuery.current
    var tweaksFiltered by remember { mutableStateOf<List<Tweak>?>(null) }

    LaunchedEffect(tweaks, settingsSearchQuery) {
        if (settingsSearchQuery.isEmpty()) {
            tweaksFiltered = null
            return@LaunchedEffect
        }

        delay(800)
        tweaksFiltered = tweaks.fastFlatMap {
            when (it) {
                is TweakGroup -> it.tweaks
                is TweakUI<*> -> listOf(it)
                else -> emptyList()
            }
        }.fastFilter {
            it.description?.contains(settingsSearchQuery, true) == true ||
                it.title.contains(settingsSearchQuery, true)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(vertical = screenPadding),
    ) {
        if (tweaksFiltered == null) {
            item(key = "Header") {
                SubScreenHeader(
                    title = title,
                    description = description,
                    modifier =
                    Modifier
                        .padding(horizontal = screenPadding)
                        .animateItem(),
                )
            }

            item {
                Spacer(modifier = Modifier.height(TweakGroupSpacing))
            }
        }

        renderTweak(tweaksFiltered ?: tweaks)
    }
}

@Composable
private fun SubScreenHeader(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(15.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = title,
            style =
                getAdaptiveTextStyle(
                    style = TypographyStyle.Headline,
                    mode = TextStyleMode.Emphasized,
                    size = 35.sp,
                ),
        )

        Text(
            text = description,
            style =
                getAdaptiveTextStyle(
                    style = TypographyStyle.Label,
                    mode = TextStyleMode.SemiEmphasized,
                ),
        )
    }
}

private fun LazyListScope.renderTweak(tweaks: List<Tweak>) {
    tweaks.fastForEachIndexed { i, tweak ->
        when (tweak) {
            is TweakUI<*> -> {
                item(key = tweak.title) {
                    RenderTweakUi(
                        tweak = tweak,
                        modifier = Modifier.animateItem(),
                    )
                }
            }

            is TweakGroup -> {
                item(key = tweak.title) {
                    val alpha by animateFloatAsState(
                        label = "alpha",
                        targetValue = if (tweak.enabled) 1F else 0.6F,
                    )

                    TitleDescriptionHeader(
                        title = tweak.title,
                        description = tweak.description,
                        titleStyle =
                            getEmphasizedLabel(
                                size = 20.sp,
                                letterSpacing = 0.1.sp,
                            ).copy(color = LocalContentColor.current.onMediumEmphasis(0.8F)),
                        modifier =
                        Modifier
                            .animateItem()
                            .alpha(alpha)
                            .padding(
                                bottom = getAdaptiveDp(10.dp),
                                top = TweakGroupSpacing,
                            )
                            .padding(horizontal = TweakPaddingHorizontal),
                    )
                }

                items(tweak.tweaks, key = { it.title }) { item ->
                    RenderTweakUi(
                        tweak = item,
                        modifier = Modifier.animateItem(),
                    )
                }

                item {
                    if (i < tweaks.lastIndex) {
                        Spacer(modifier = Modifier.height(getAdaptiveDp(TweakGroupSpacing)))
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderTweakUi(
    tweak: TweakUI<*>,
    modifier: Modifier = Modifier,
) {
    val scope = AppDispatchers.IO.scope
    val icon = tweak.iconId?.let { painterResource(it) }

    when (tweak) {
        is TweakUI.Divider -> {
            HorizontalDivider(
                thickness = 1.dp,
                color = LocalContentColor.current.onMediumEmphasis(0.3F),
                modifier =
                    modifier
                        .padding(
                            horizontal = getAdaptiveDp(TweakPaddingHorizontal),
                            vertical = getAdaptiveDp(TweakGroupSpacing / 2),
                        ),
            )
        }

        is TweakUI.InformationTweak -> {
            BaseTweakComponent(
                modifier = modifier,
                title = tweak.title,
                description = tweak.description,
                enabled = tweak.enabled,
                startContent = {
                    AdaptiveIcon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = LocalContentColor.current.onMediumEmphasis(),
                    )
                },
            )
        }

        is TweakUI.SliderTweak -> {
            SliderComponent(
                modifier = modifier,
                title = tweak.title,
                description = tweak.description,
                icon = icon,
                selectedValueProvider = { tweak.value.value },
                range = tweak.range,
                steps = tweak.steps,
                enabled = tweak.enabled,
                onValueChange = {
                    scope.launch {
                        if (tweak.onTweaked(it)) {
                            tweak.value.value = it
                        }
                    }
                },
            )
        }

        is TweakUI.SwitchTweak -> {
            SwitchComponent(
                modifier = modifier,
                title = tweak.title,
                description = tweak.description,
                icon = icon,
                enabled = tweak.enabled,
                checked = tweak.value.value,
                onCheckedChanged = {
                    scope.launch {
                        if (tweak.onTweaked(it)) {
                            tweak.value.value = it
                        }
                    }
                },
            )
        }

        is TweakUI.TextFieldTweak -> {
            TextFieldComponent(
                modifier = modifier,
                title = tweak.title,
                description = tweak.description,
                icon = icon,
                value = tweak.value.value,
                enabled = tweak.enabled,
                onValueChange = {
                    scope.launch {
                        if (tweak.onTweaked(it)) {
                            tweak.value.value = it
                        }
                    }
                },
            )
        }

        is TweakUI.ListTweak<*> -> {
            ListRadioComponent(
                modifier = modifier,
                title = tweak.title,
                description = tweak.description,
                icon = icon,
                endContent = tweak.endContent,
                options = tweak.options,
                selectedValue = tweak.value.value,
                enabled = tweak.enabled,
                onValueChange = {
                    scope.launch {
                        if (tweak.internalOnValueChanged(it!!)) {
                            tweak.internalSet(it)
                        }
                    }
                },
            )
        }

        is TweakUI.MultiSelectListTweak<*> -> {
            ListSelectComponent(
                modifier = modifier,
                title = tweak.title,
                description = tweak.description,
                icon = icon,
                endContent = tweak.endContent,
                options = tweak.options,
                selectedValues = tweak.values.value,
                enabled = tweak.enabled,
                onValueChange = {
                    scope.launch {
                        if (tweak.internalOnValueChanged(it)) {
                            tweak.internalSet(it)
                        }
                    }
                },
            )
        }

        is TweakUI.ClickableTweak -> {
            ClickableComponent(
                modifier = modifier,
                title = tweak.title,
                description = tweak.description,
                icon = icon,
                onClick = tweak.onClick,
                enabled = tweak.enabled,
            )
        }

        is TweakUI.DialogTweak -> {
            DialogComponent(
                modifier = modifier,
                title = tweak.title,
                description = tweak.description,
                dialogTitle = tweak.dialogTitle,
                dialogMessage = tweak.dialogMessage,
                icon = icon,
                enabled = tweak.enabled,
                onConfirm = tweak.onConfirm,
            )
        }

        is TweakUI.CustomContentTweak -> {
            tweak.content()
        }
    }
}
