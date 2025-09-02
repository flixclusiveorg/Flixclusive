package com.flixclusive.feature.mobile.settings

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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastFlatMap
import androidx.compose.ui.util.fastForEachIndexed
import com.flixclusive.core.ui.common.adaptive.AdaptiveIcon
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveTextStyle
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveTextStyle
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
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

    LaunchedEffect(settingsSearchQuery) {
        snapshotFlow {
            settingsSearchQuery.value
        }.distinctUntilChanged()
            .collectLatest { query ->
                if (query.isEmpty()) {
                    tweaksFiltered = null
                    return@collectLatest
                }

                delay(800)
                tweaksFiltered =
                    tweaks
                        .fastFlatMap {
                            when (it) {
                                is TweakGroup -> it.tweaks
                                is TweakUI<*> -> listOf(it)
                                else -> emptyList()
                            }
                        }.fastFilter {
                            it.descriptionProvider?.invoke()?.contains(query, true) == true ||
                                it.title.contains(query, true)
                        }
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
                    style = AdaptiveTextStyle.Emphasized,
                    size = 35.sp,
                ),
        )

        Text(
            text = description,
            style =
                getAdaptiveTextStyle(
                    style = TypographyStyle.Label,
                    style = AdaptiveTextStyle.SemiEmphasized,
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
                    TitleDescriptionHeader(
                        title = tweak.title,
                        descriptionProvider = tweak.descriptionProvider,
                        titleStyle =
                            getEmphasizedLabel(
                                size = 20.sp,
                                letterSpacing = 0.1.sp,
                            ).copy(color = LocalContentColor.current.copy(0.8F)),
                        modifier =
                            Modifier
                                .animateItem()
                                .graphicsLayer {
                                    alpha = if (tweak.enabledProvider()) 1F else 0.6F
                                }.padding(
                                    bottom = getAdaptiveDp(10.dp),
                                    top = TweakGroupSpacing,
                                ).padding(horizontal = TweakPaddingHorizontal),
                    )
                }

                items(tweak.tweaks, key = { it.title }) { item ->
                    RenderTweakUi(
                        tweak = item,
                        modifier = Modifier.animateItem(),
                    )
                }

                item(key = "${tweak.title}-$i-divider") {
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
                color = LocalContentColor.current.copy(0.3F),
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
                descriptionProvider = tweak.descriptionProvider,
                enabledProvider = tweak.enabledProvider,
                startContent = {
                    AdaptiveIcon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = LocalContentColor.current.copy(0.6f),
                    )
                },
            )
        }

        is TweakUI.SliderTweak -> {
            SliderComponent(
                modifier = modifier,
                title = tweak.title,
                descriptionProvider = tweak.descriptionProvider,
                icon = icon,
                selectedValueProvider = { tweak.value.value },
                range = tweak.range,
                steps = tweak.steps,
                enabledProvider = tweak.enabledProvider,
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
                descriptionProvider = tweak.descriptionProvider,
                icon = icon,
                enabledProvider = tweak.enabledProvider,
                checked = { tweak.value.value },
                onCheckedChange = {
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
                descriptionProvider = tweak.descriptionProvider,
                icon = icon,
                valueProvider = { tweak.value.value },
                enabledProvider = tweak.enabledProvider,
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
                descriptionProvider = tweak.descriptionProvider,
                icon = icon,
                endContent = tweak.endContent,
                options = tweak.options,
                selectedValueProvider = { tweak.value.value },
                enabledProvider = tweak.enabledProvider,
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
                descriptionProvider = tweak.descriptionProvider,
                icon = icon,
                endContent = tweak.endContent,
                options = tweak.options,
                selectedValuesProvider = { tweak.values.value },
                enabledProvider = tweak.enabledProvider,
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
                descriptionProvider = tweak.descriptionProvider,
                icon = icon,
                onClick = tweak.onClick,
                enabledProvider = tweak.enabledProvider,
            )
        }

        is TweakUI.DialogTweak -> {
            DialogComponent(
                modifier = modifier,
                title = tweak.title,
                descriptionProvider = tweak.descriptionProvider,
                dialogTitle = tweak.dialogTitle,
                dialogMessage = tweak.dialogMessage,
                icon = icon,
                enabledProvider = tweak.enabledProvider,
                onConfirm = tweak.onConfirm,
            )
        }

        is TweakUI.CustomContentTweak -> {
            tweak.content()
        }
    }
}
