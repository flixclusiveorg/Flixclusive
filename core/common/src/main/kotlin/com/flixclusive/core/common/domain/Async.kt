package com.flixclusive.core.common.domain

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.flixclusive.core.common.locale.UiText

sealed class Async<out T> {
    data object Loading : Async<Nothing>()

    @Stable
    data class Success<T>(
        val data: T
    ) : Async<T>() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Success<*>) return false
            return data == other.data
        }

        override fun hashCode(): Int {
            return data?.hashCode() ?: 0
        }
    }

    @Stable
    data class Failure(
        val message: UiText,
        val cause: Throwable? = null,
    ) : Async<Nothing>() {
        constructor(cause: Throwable) : this(
            message = UiText.from(cause.message ?: "An unknown error occurred"),
            cause = cause,
        )

        constructor(message: String) : this(
            message = UiText.from(message),
            cause = null,
        )

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Failure) return false
            return message == other.message && cause?.message == other.cause?.message
        }

        override fun hashCode(): Int {
            var result = message.hashCode()
            result = 31 * result + (cause?.hashCode() ?: 0)
            return result
        }
    }

    val isLoading: Boolean get() = this is Loading
    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    companion object {
        // Sealed class that carries NO data — pure discriminator
        sealed interface AsyncType {
            data object Loading : AsyncType

            data object Failure : AsyncType

            data object Success : AsyncType
        }

        @Composable
        fun <S> AsyncAnimatedContent(
            targetState: Async<S>,
            loadingContent: @Composable AnimatedContentScope.() -> Unit,
            errorContent: @Composable AnimatedContentScope.(targetState: Failure) -> Unit,
            modifier: Modifier = Modifier,
            transitionSpec: AnimatedContentTransitionScope<AsyncType>.() -> ContentTransform = {
                (fadeIn(animationSpec = tween(220, delayMillis = 90)))
                    .togetherWith(fadeOut(animationSpec = tween(90)))
            },
            contentAlignment: Alignment = Alignment.TopStart,
            label: String = "AsyncAnimatedContent",
            content: @Composable AnimatedContentScope.(targetState: () -> S) -> Unit,
        ) {
            val targetType = when (targetState) {
                is Loading -> AsyncType.Loading
                is Failure -> AsyncType.Failure
                is Success -> AsyncType.Success
            }

            AnimatedContent(
                targetState = targetType,
                transitionSpec = transitionSpec,
                contentAlignment = contentAlignment,
                label = label,
                modifier = modifier
            ) { type ->
                when (type) {
                    is AsyncType.Loading -> {
                        loadingContent()
                    }

                    is AsyncType.Failure -> {
                        val failureState = (targetState as? Failure) ?: return@AnimatedContent
                        errorContent(failureState)
                    }

                    is AsyncType.Success -> {
                        val successState = (targetState as? Success<S>)?.data ?: return@AnimatedContent
                        content { successState }
                    }
                }
            }
        }
    }
}
