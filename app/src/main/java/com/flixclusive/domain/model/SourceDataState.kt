package com.flixclusive.domain.model

import androidx.annotation.StringRes
import com.flixclusive.R
import com.flixclusive.common.UiText

sealed class SourceDataState(val message: UiText) {
    companion object {
        @StringRes private val defaultUnavailableMessageId = R.string.video_data_dialog_state_unavailable_default
        @StringRes private val defaultErrorMessageId = R.string.video_data_dialog_state_error_default
    }
    
    data object Idle : SourceDataState(message = UiText.StringValue(""))

    class Fetching(
        message: UiText? = null,
    ) : SourceDataState(
        message = message
            ?: UiText.StringResource(R.string.video_data_dialog_state_fetching)
    ) {
        constructor(message: String) : this(UiText.StringValue(message))
        constructor(@StringRes errorMessageId: Int) : this(
            message = UiText.StringResource(errorMessageId)
        )
    }

    class Extracting(
        message: UiText? = null,
    ) : SourceDataState(
        message = message
            ?: UiText.StringResource(R.string.video_data_dialog_state_extracting)
    ) {
        constructor(message: String) : this(UiText.StringValue(message))
    }

    class Error(
        errorMessage: UiText? = null,
    ) : SourceDataState(
        message = errorMessage
            ?: UiText.StringResource(defaultErrorMessageId)
    ) {
        constructor(@StringRes errorMessageId: Int) : this(
            errorMessage = UiText.StringResource(errorMessageId)
        )
    }

    class Unavailable(errorMessage: UiText? = null) :
        SourceDataState(
            message = errorMessage ?: UiText.StringResource(defaultUnavailableMessageId)
        ) {
        constructor(@StringRes errorMessageId: Int) : this(
            errorMessage = UiText.StringResource(errorMessageId)
        )
    }

    object Success :
        SourceDataState(message = UiText.StringResource(R.string.video_data_dialog_state_success))
}