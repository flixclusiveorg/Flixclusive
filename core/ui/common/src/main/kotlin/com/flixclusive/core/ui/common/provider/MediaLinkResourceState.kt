package com.flixclusive.core.ui.common.provider

import androidx.annotation.StringRes
import com.flixclusive.core.locale.UiText
import com.flixclusive.core.ui.common.provider.MediaLinkResourceState.Error
import com.flixclusive.core.ui.common.provider.MediaLinkResourceState.Extracting
import com.flixclusive.core.ui.common.provider.MediaLinkResourceState.Fetching
import com.flixclusive.core.ui.common.provider.MediaLinkResourceState.Idle
import com.flixclusive.core.ui.common.provider.MediaLinkResourceState.Success
import com.flixclusive.core.ui.common.provider.MediaLinkResourceState.Unavailable
import com.flixclusive.core.util.R as UtilR


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
    constructor(message: String) : this(UiText.StringValue(message))
    constructor() : this(UiText.StringValue(""))

    companion object {
        @StringRes
        private val defaultUnavailableMessageId = UtilR.string.source_data_dialog_state_unavailable_default

        @StringRes
        private val defaultErrorMessageId = UtilR.string.source_data_dialog_state_error_default
    }

    /**
     * The initial idle state.
     */
    data object Idle : MediaLinkResourceState()

    /**
     * The state when the resource is being fetched.
     *
     * @param message An optional message to display while fetching.
     */
    class Fetching(
        message: UiText? = null,
    ) : MediaLinkResourceState(
        message = message
            ?: UiText.StringResource(UtilR.string.source_data_dialog_state_fetching)
    ) {
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
            ?: UiText.StringResource(UtilR.string.source_data_dialog_state_extracting)
    ) {
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
    data object Success :
        MediaLinkResourceState(
            message = UiText.StringResource(UtilR.string.source_data_dialog_state_success)
        )
}