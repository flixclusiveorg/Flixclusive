package com.flixclusive.core.ui.common.provider

import androidx.annotation.StringRes
import com.flixclusive.core.locale.UiText
import com.flixclusive.core.ui.common.provider.MediaLinkResourceState.Error
import com.flixclusive.core.ui.common.provider.MediaLinkResourceState.Extracting
import com.flixclusive.core.ui.common.provider.MediaLinkResourceState.Fetching
import com.flixclusive.core.ui.common.provider.MediaLinkResourceState.Idle
import com.flixclusive.core.ui.common.provider.MediaLinkResourceState.Success
import com.flixclusive.core.ui.common.provider.MediaLinkResourceState.Unavailable
import com.flixclusive.model.provider.link.MediaLink
import com.flixclusive.core.locale.R as LocaleR


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
sealed class MediaLinkResourceState(val message: UiText) {
    protected abstract val ordinal: Int

    constructor(message: String) : this(UiText.StringValue(message))
    constructor() : this(UiText.StringValue(""))

    companion object {
        @StringRes
        private val defaultUnavailableMessageId = LocaleR.string.source_data_dialog_state_unavailable_default

        @StringRes
        private val defaultErrorMessageId = LocaleR.string.source_data_dialog_state_error_default
    }

    /**
     * The initial idle state.
     */
    data object Idle : MediaLinkResourceState() {
        override val ordinal = 0
    }

    /**
     * The state when the resource is being fetched.
     *
     * @param message An optional message to display while fetching.
     */
    class Fetching(
        message: UiText? = null,
    ) : MediaLinkResourceState(
        message = message
            ?: UiText.StringResource(LocaleR.string.source_data_dialog_state_fetching)
    ) {
        override val ordinal = 1

        /**
         * Constructor with a string resource ID for the message.
         *
         * @param errorMessageId The string resource ID for the message.
         */
        constructor(@StringRes errorMessageId: Int) : this(
            message = UiText.StringResource(errorMessageId)
        )
    }

    /**
     * The state when the resource is being extracted.
     *
     * @param message An optional message to display while extracting.
     */
    class Extracting(
        message: UiText? = null,
    ) : MediaLinkResourceState(
        message = message
            ?: UiText.StringResource(LocaleR.string.source_data_dialog_state_extracting)
    ) {
        override val ordinal = 2
        /**
         * Constructor with a custom string message.
         *
         * @param message The custom message to display.
         */
        constructor(message: String) : this(UiText.StringValue(message))
    }

    /**
     * The state when an error occurred while fetching or extracting the resource.
     *
     * @param errorMessage An optional error message to display.
     */
    class Error(
        errorMessage: UiText? = null,
    ) : MediaLinkResourceState(
        message = errorMessage
            ?: UiText.StringResource(defaultErrorMessageId)
    ) {
        override val ordinal = 3
        /**
         * Constructor with a string resource ID for the error message.
         *
         * @param errorMessageId The string resource ID for the error message.
         */
        constructor(@StringRes errorMessageId: Int) : this(
            errorMessage = UiText.StringResource(errorMessageId)
        )

        constructor(error: Throwable?) : this(
            if (error?.localizedMessage == null) UiText.StringResource(defaultErrorMessageId)
            else UiText.StringValue(error.localizedMessage!!)
        )
    }

    /**
     * The state when the resource is unavailable.
     *
     * @param errorMessage An optional error message to display.
     */
    class Unavailable(errorMessage: UiText? = null) :
        MediaLinkResourceState(
            message = errorMessage
                ?: UiText.StringResource(defaultUnavailableMessageId)
        ) {
        override val ordinal = 4
        /**
         * Constructor with a string resource ID for the error message.
         *
         * @param errorMessageId The string resource ID for the error message.
         */
        constructor(@StringRes errorMessageId: Int) : this(
            errorMessage = UiText.StringResource(errorMessageId)
        )
    }

    /**
     * The state when the resource has been successfully fetched and extracted.
     */
    data object Success : MediaLinkResourceState(message = UiText.StringResource(LocaleR.string.source_data_dialog_state_success)) {
        override val ordinal = 5
    }

    /**
     * The state when the resource has been successfully fetched and extracted from trusted providers.
     */
    data object SuccessWithTrustedProviders : MediaLinkResourceState(
        message = UiText.StringResource(LocaleR.string.source_data_dialog_state_success_with_trusted_providers)
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

    operator fun compareTo(other: MediaLinkResourceState): Int {
        return ordinal.compareTo(other.ordinal)
    }
}