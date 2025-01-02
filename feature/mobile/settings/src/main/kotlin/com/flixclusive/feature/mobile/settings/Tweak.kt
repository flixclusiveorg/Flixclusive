package com.flixclusive.feature.mobile.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap

/**
 *
 * A tweak represents a settings item in different modes such as groups or individual UIs.
 * */
sealed class Tweak {
    abstract val title: String
    abstract val description: String?
    abstract val enabled: Boolean
}

data class TweakGroup(
    override val title: String,
    override val description: String? = null,
    override val enabled: Boolean = true,
    val tweaks: ImmutableList<TweakUI<out Any>>
) : Tweak()

/**
 * A set of settings items in different UI modes.
 * */
sealed class TweakUI<T> : Tweak() {
    abstract val onTweaked: suspend (newValue: T) -> Boolean
    abstract val iconId: Int?

    /**
     * A tweak that is only used for displaying texts
     * */
    data class InformationTweak(
        override val title: String,
        override val description: String? = null
    ) : TweakUI<Unit>() {
        override val enabled: Boolean = true
        override val onTweaked: suspend (newValue: Unit) -> Boolean = { true }
        override val iconId: Int? = null
    }

    /**
     * A tweak that is only used for custom onClick events
     * */
    data class ClickableTweak(
        override val title: String,
        override val description: String? = null,
        override val iconId: Int? = null,
        override val enabled: Boolean = true,
        val onClick: () -> Unit,
    ) : TweakUI<Unit>() {
        override val onTweaked: suspend (newValue: Unit) -> Boolean = { true }
    }

    data class SwitchTweak(
        val value: MutableState<Boolean>,
        override val title: String,
        override val description: String? = null,
        override val iconId: Int? = null,
        override val enabled: Boolean = true,
        override val onTweaked: suspend (newValue: Boolean) -> Boolean = { true }
    ) : TweakUI<Boolean>()

    data class SliderTweak(
        val value: MutableState<Float>,
        val range: ClosedFloatingPointRange<Float> = 0F..1F,
        val steps: Int = 0,
        override val title: String,
        override val description: String? = null,
        override val iconId: Int? = null,
        override val enabled: Boolean = true,
        override val onTweaked: suspend (newValue: Float) -> Boolean = { true }
    ) : TweakUI<Float>()

    data class ListTweak<S>(
        val value: MutableState<S>,
        val options: ImmutableMap<S, String>,
        val endContent: @Composable (() -> Unit)? = null,
        override val title: String,
        override val description: String? = null,
        override val iconId: Int? = null,
        override val enabled: Boolean = true,
        override val onTweaked: suspend (newValue: S) -> Boolean = { true }
    ) : TweakUI<S>() {
        internal fun internalSet(newValue: Any) {
            value.value = newValue as S
        }
        internal suspend fun internalOnValueChanged(newValue: Any) = onTweaked(newValue as S)
    }

    data class MultiSelectListTweak<S>(
        val values: MutableState<Set<S>>,
        val options: ImmutableMap<S, String>,
        val endContent: @Composable (() -> Unit)? = null,
        override val title: String,
        override val description: String? = null,
        override val iconId: Int? = null,
        override val enabled: Boolean = true,
        override val onTweaked: suspend (newValue: Set<S>) -> Boolean = { true }
    ) : TweakUI<Set<S>>() {
        internal fun internalSet(newValue: Set<Any?>) {
            values.value = newValue as Set<S>
        }
        internal suspend fun internalOnValueChanged(newValue: Set<Any?>) = onTweaked(newValue as Set<S>)
    }

    data class TextFieldTweak(
        val value: MutableState<String>,
        override val title: String,
        override val description: String? = null,
        override val iconId: Int? = null,
        override val enabled: Boolean = true,
        override val onTweaked: suspend (newValue: String) -> Boolean = { true }
    ) : TweakUI<String>()

    data class CustomContentTweak<T>(
        val value: MutableState<T>,
        override val title: String,
        override val description: String? = null,
        override val iconId: Int? = null,
        override val enabled: Boolean = true,
        override val onTweaked: suspend (newValue: T) -> Boolean = { true },
        val content: @Composable () -> Unit
    ) : TweakUI<T>()
}