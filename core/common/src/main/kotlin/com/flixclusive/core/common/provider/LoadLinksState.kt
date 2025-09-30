package com.flixclusive.core.common.provider

import androidx.annotation.StringRes
import com.flixclusive.core.common.R
import com.flixclusive.core.common.exception.ExceptionWithUiText
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.common.provider.LoadLinksState.Error
import com.flixclusive.core.common.provider.LoadLinksState.Extracting
import com.flixclusive.core.common.provider.LoadLinksState.Fetching
import com.flixclusive.core.common.provider.LoadLinksState.Idle
import com.flixclusive.core.common.provider.LoadLinksState.Success
import com.flixclusive.core.common.provider.LoadLinksState.Unavailable
import com.flixclusive.model.provider.link.MediaLink

/**
 * Represents the state of a media link resource.
 *
 * @param message A [UiText] message describing the current state.
 *
 * @see Idle
 * @see Fetching
 * @see Extracting
 * @see Error
 * @see Unavailable
 * @see Success
 * @see MediaLink
 */
sealed class LoadLinksState(
    val message: UiText,
) {
    protected abstract val ordinal: Int

    constructor(message: String) : this(UiText.from(message))
    constructor() : this(UiText.from(""))

    companion object {
        @StringRes
        private val defaultUnavailableMessageId = R.string.source_data_dialog_state_unavailable_default

        @StringRes
        private val defaultErrorMessageId = R.string.source_data_dialog_state_error_default
    }

    /**
     * The initial idle state.
     */
    data object Idle : LoadLinksState() {
        override val ordinal = 0
    }

    /**
     * The state when the resource is being fetched.
     *
     * @param message An optional message to display while fetching.
     */
    class Fetching(
        message: UiText? = null,
    ) : LoadLinksState(
            message = message
                ?: UiText.from(R.string.source_data_dialog_state_fetching),
        ) {
        override val ordinal = 1

        /**
         * Constructor with a string resource ID for the message.
         *
         * @param errorMessageId The string resource ID for the message.
         */
        constructor(
            @StringRes errorMessageId: Int,
        ) : this(
            message = UiText.from(errorMessageId),
        )
    }

    /**
     * The state when the resource is being extracted.
     *
     * @param message An optional message to display while extracting.
     */
    class Extracting(
        message: UiText? = null,
    ) : LoadLinksState(
            message = message
                ?: UiText.from(R.string.source_data_dialog_state_extracting),
        ) {
        override val ordinal = 2

        /**
         * Constructor with a custom string message.
         *
         * @param message The custom message to display.
         */
        constructor(message: String) : this(UiText.from(message))
    }

    /**
     * The state when an error occurred while fetching or extracting the resource.
     *
     * @param errorMessage An optional error message to display.
     */
    class Error(
        errorMessage: UiText? = null,
    ) : LoadLinksState(
            message = errorMessage
                ?: UiText.from(defaultErrorMessageId),
        ) {
        override val ordinal = 3

        /**
         * Constructor with a string resource ID for the error message.
         *
         * @param errorMessageId The string resource ID for the error message.
         */
        constructor(
            @StringRes errorMessageId: Int,
        ) : this(
            errorMessage = UiText.from(errorMessageId),
        )

        constructor(error: Throwable?) : this(
            if (error is ExceptionWithUiText) {
                error.uiText
            } else if (error?.localizedMessage == null) {
                UiText.from(defaultErrorMessageId)
            } else {
                UiText.from(error.localizedMessage!!)
            },
        )
    }

    /**
     * The state when the resource is unavailable.
     *
     * @param errorMessage An optional error message to display.
     */
    class Unavailable(
        errorMessage: UiText? = null,
    ) : LoadLinksState(
            message = errorMessage
                ?: UiText.from(defaultUnavailableMessageId),
        ) {
        override val ordinal = 4

        /**
         * Constructor with a string resource ID for the error message.
         *
         * @param errorMessageId The string resource ID for the error message.
         */
        constructor(
            @StringRes errorMessageId: Int,
        ) : this(
            errorMessage = UiText.from(errorMessageId),
        )
    }

    /**
     * The state when the resource has been successfully fetched and extracted.
     */
    data object Success :
        LoadLinksState(message = UiText.from(R.string.source_data_dialog_state_success)) {
        override val ordinal = 5
    }

    /**
     * The state when the resource has been successfully fetched and extracted from trusted providers.
     */
    data object SuccessWithTrustedProviders : LoadLinksState(
        message = UiText.from(R.string.source_data_dialog_state_success_with_trusted_providers),
    ) {
        override val ordinal = 6
    }

    val isIdle get() = this is Idle
    val isSuccess get() = this is Success
    val isSuccessWithTrustedProviders get() = this is SuccessWithTrustedProviders
    val isUnavailable get() = this is Unavailable
    val isExtracting get() = this is Extracting
    val isFetching get() = this is Fetching
    val isLoading get() = isFetching || isExtracting || isSuccess
    val isError get() = this is Error || isUnavailable

    operator fun compareTo(other: LoadLinksState): Int {
        return ordinal.compareTo(other.ordinal)
    }
}
