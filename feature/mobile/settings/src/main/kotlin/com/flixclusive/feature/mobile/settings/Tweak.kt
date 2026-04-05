package com.flixclusive.feature.mobile.settings

import androidx.compose.runtime.Composable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlin.random.Random

/**
 *
 * A tweak represents a settings item in different modes such as groups or individual UIs.
 *
 * Necessary to use deferred on some variables here such as enabled and description.
 * */
sealed class Tweak {
    abstract val title: String
    abstract val description: (() -> String)?
    abstract val enabledProvider: () -> Boolean
}

data class TweakGroup(
    override val title: String,
    override val description: (() -> String)? = null,
    override val enabledProvider: () -> Boolean = { true },
    val tweaks: ImmutableList<TweakUI<out Any>>,
) : Tweak()

/**
 * A set of settings items in different UI modes.
 * */
sealed class TweakUI<T> : Tweak() {
    abstract val onTweaked: (newValue: T) -> Unit
    abstract val iconId: Int?

    /**
     * A tweak that is only used for displaying texts
     * */
    data class InformationTweak(
        override val title: String,
        override val description: (() -> String)? = null,
    ) : TweakUI<Unit>() {
        override val enabledProvider: () -> Boolean = { true }
        override val onTweaked: (newValue: Unit) -> Unit = { /*No-op*/ }
        override val iconId: Int? = null
    }

    /**
     * A tweak that is only used for displaying texts
     * */
    class Divider : TweakUI<Unit>() {
        override val title: String by lazy { Random.nextDouble().toString() }
        override val description: (() -> String)? = null
        override val enabledProvider: () -> Boolean = { true }
        override val onTweaked: (newValue: Unit) -> Unit = { /*No-op*/ }
        override val iconId: Int? = null
    }

    /**
     * A tweak that is only used for custom onClick events
     * */
    data class ClickableTweak(
        override val title: String,
        override val description: (() -> String)? = null,
        override val iconId: Int? = null,
        override val enabledProvider: () -> Boolean = { true },
        val onClick: () -> Unit,
    ) : TweakUI<Unit>() {
        override val onTweaked: (newValue: Unit) -> Unit = { /*No-op*/ }
    }

    data class DialogTweak(
        override val title: String,
        override val description: (() -> String)? = null,
        override val iconId: Int? = null,
        override val enabledProvider: () -> Boolean = { true },
        val dismissOnConfirm: Boolean = true,
        val dialogTitle: String = title,
        val dialogMessage: String,
        val onConfirm: () -> Unit,
    ) : TweakUI<Unit>() {
        override val onTweaked: (newValue: Unit) -> Unit = { /*No-op*/ }
    }

    data class SwitchTweak(
        val value: () -> Boolean,
        override val title: String,
        override val description: (() -> String)? = null,
        override val iconId: Int? = null,
        override val enabledProvider: () -> Boolean = { true },
        override val onTweaked: (newValue: Boolean) -> Unit = { /*No-op*/ }
    ) : TweakUI<Boolean>()

    data class SliderTweak(
        val value: () -> Float,
        val range: ClosedFloatingPointRange<Float> = 0F..1F,
        val steps: Int = 0,
        override val title: String,
        override val description: (() -> String)? = null,
        override val iconId: Int? = null,
        override val enabledProvider: () -> Boolean = { true },
        override val onTweaked: (newValue: Float) -> Unit = { /*No-op*/ }
    ) : TweakUI<Float>()

    @Suppress("UNCHECKED_CAST")
    data class ListTweak<S>(
        val value: () -> S,
        val options: ImmutableMap<S, String>,
        val endContent: @Composable (() -> Unit)? = null,
        override val title: String,
        override val description: (() -> String)? = null,
        override val iconId: Int? = null,
        override val enabledProvider: () -> Boolean = { true },
        override val onTweaked: (newValue: S) -> Unit = { /*No-op*/ }
    ) : TweakUI<S>()

    @Suppress("UNCHECKED_CAST")
    data class MultiSelectListTweak<S>(
        val values: Set<S>,
        val options: ImmutableMap<S, String>,
        val endContent: @Composable (() -> Unit)? = null,
        override val title: String,
        override val description: (() -> String)? = null,
        override val iconId: Int? = null,
        override val enabledProvider: () -> Boolean = { true },
        override val onTweaked: (newValue: Set<S>) -> Unit = { /*No-op*/ }
    ) : TweakUI<Set<S>>()

    data class TextFieldTweak(
        val value: () -> String,
        override val title: String,
        override val description: (() -> String)? = null,
        override val iconId: Int? = null,
        override val enabledProvider: () -> Boolean = { true },
        override val onTweaked: (newValue: String) -> Unit = { /*No-op*/ }
    ) : TweakUI<String>()

    data class CustomContentTweak(
        override val title: String,
        override val description: (() -> String)? = null,
        val content: @Composable () -> Unit,
    ) : TweakUI<Nothing>() {
        override val onTweaked: (Nothing) -> Unit = { /*No-op*/ }
        override val iconId: Int? get() = null
        override val enabledProvider: () -> Boolean = { true }
    }
}
