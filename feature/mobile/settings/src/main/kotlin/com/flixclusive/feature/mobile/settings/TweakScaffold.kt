package com.flixclusive.feature.mobile.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.adaptive.AdaptiveIcon
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveTextStyle
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.core.ui.common.util.adaptive.TextStyleMode
import com.flixclusive.core.ui.common.util.adaptive.TypographyStyle
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.feature.mobile.settings.component.BaseTweakComponent
import com.flixclusive.feature.mobile.settings.component.ClickableComponent
import com.flixclusive.feature.mobile.settings.component.GroupLabel
import com.flixclusive.feature.mobile.settings.component.ListRadioComponent
import com.flixclusive.feature.mobile.settings.component.ListSelectComponent
import com.flixclusive.feature.mobile.settings.component.SliderComponent
import com.flixclusive.feature.mobile.settings.component.SwitchComponent
import com.flixclusive.feature.mobile.settings.component.TextFieldComponent
import com.flixclusive.feature.mobile.settings.screen.advanced.AdvancedTweakScreen
import com.flixclusive.feature.mobile.settings.util.UiUtil.getEmphasizedLabel
import kotlinx.coroutines.launch

internal val TweakPaddingHorizontal = 10.dp
private val TweakGroupSpacing = 25.dp

@Composable
internal fun TweakScaffold(
    title: String,
    description: String,
    tweaksProvider: @Composable () -> List<Tweak>
) {
    val tweaks = tweaksProvider()
    val screenPadding = getAdaptiveDp(TweakPaddingHorizontal)
    LazyColumn(
        contentPadding = PaddingValues(vertical = screenPadding)
    ) {
        item {
            SubScreenHeader(
                title = title,
                description = description,
                modifier = Modifier
                    .padding(horizontal = screenPadding)
            )
        }

        item {
            Spacer(modifier = Modifier.height(TweakGroupSpacing))
        }

        renderTweak(tweaks)
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
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = getAdaptiveTextStyle(
                style = TypographyStyle.Headline,
                mode = TextStyleMode.Emphasized,
                size = 35.sp
            )
        )

        Text(
            text = description,
            style = getAdaptiveTextStyle(
                style = TypographyStyle.Label,
                mode = TextStyleMode.SemiEmphasized,
            )
        )
    }
}

private fun LazyListScope.renderTweak(tweaks: List<Tweak>) {
    tweaks.fastForEachIndexed { i, tweak ->
        when (tweak) {
            is TweakUI<*> -> {
                item {
                    RenderTweakUi(tweak)
                }
            }
            is TweakGroup -> {
                item {
                    val alpha by animateFloatAsState(
                        label = "alpha",
                        targetValue = if (tweak.enabled) 1F else 0.6F,
                    )

                    GroupLabel(
                        title = tweak.title,
                        description = tweak.description,
                        titleStyle = getEmphasizedLabel(
                            size = 20.sp, letterSpacing = 0.1.sp,
                        ).copy(color = LocalContentColor.current.onMediumEmphasis(0.8F)),
                        modifier = Modifier
                            .alpha(alpha)
                            .padding(
                                bottom = getAdaptiveDp(10.dp),
                                top = TweakGroupSpacing
                            )
                            .padding(horizontal = TweakPaddingHorizontal)
                    )
                }

                items(tweak.tweaks) { item ->
                    RenderTweakUi(tweak = item)
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
private fun RenderTweakUi(tweak: TweakUI<*>) {
    val scope = rememberCoroutineScope()
    val icon = tweak.iconId?.let { painterResource(it) }

    when (tweak) {
        is TweakUI.InformationTweak -> {
            BaseTweakComponent(
                title = tweak.title,
                description = tweak.description,
                enabled = tweak.enabled,
                startContent = {
                    AdaptiveIcon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = LocalContentColor.current.onMediumEmphasis()
                    )
                }
            )
        }
        is TweakUI.SliderTweak -> {
            SliderComponent(
                title = tweak.title,
                description = tweak.description,
                icon = icon,
                selectedValue = tweak.value.value,
                range = tweak.range,
                enabled = tweak.enabled,
                onValueChange = {
                    scope.launch {
                        if (tweak.onTweaked(it)) {
                            tweak.value.value = it
                        }
                    }
                }
            )
        }
        is TweakUI.SwitchTweak -> {
            SwitchComponent(
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
                }
            )
        }
        is TweakUI.TextFieldTweak -> {
            TextFieldComponent(
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
                }
            )
        }
        is TweakUI.ListTweak<*> -> {
            ListRadioComponent(
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
                }
            )
        }
        is TweakUI.MultiSelectListTweak<*> -> {
            ListSelectComponent(
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
                }
            )
        }
        is TweakUI.ClickableTweak -> {
            ClickableComponent(
                title = tweak.title,
                description = tweak.description,
                icon = icon,
                onClick = tweak.onClick,
                enabled = tweak.enabled,
            )
        }
        is TweakUI.CustomContentTweak<*> -> {
            tweak.content()
        }
    }
}

@Preview
@Composable
private fun TweakScaffoldBasePreview() {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
        ) {
            TweakScaffold(
                title = AdvancedTweakScreen.getTitle(),
                description = AdvancedTweakScreen.getDescription(),
                tweaksProvider = { AdvancedTweakScreen.getTweaks() }
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun TweakScaffoldCompactLandscapePreview() {
    TweakScaffoldBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun TweakScaffoldMediumPortraitPreview() {
    TweakScaffoldBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun TweakScaffoldMediumLandscapePreview() {
    TweakScaffoldBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun TweakScaffoldExtendedPortraitPreview() {
    TweakScaffoldBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun TweakScaffoldExtendedLandscapePreview() {
    TweakScaffoldBasePreview()
}