package com.flixclusive.model.provider

import androidx.annotation.StringRes
import com.flixclusive.core.util.common.ui.UiText
import com.flixclusive.core.util.R as UtilR


sealed class SourceDataState(val message: UiText) {
    companion object {
        @StringRes private val defaultUnavailableMessageId = UtilR.string.source_data_dialog_state_unavailable_default
        @StringRes private val defaultErrorMessageId = UtilR.string.source_data_dialog_state_error_default
    }
    
    data object Idle : SourceDataState(message = UiText.StringValue(""))

    class Fetching(
        message: UiText? = null,
    ) : SourceDataState(
        message = message
            ?: UiText.StringResource(UtilR.string.source_data_dialog_state_fetching)
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
            ?: UiText.StringResource(UtilR.string.source_data_dialog_state_extracting)
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

    data object Success :
        SourceDataState(message = UiText.StringResource(UtilR.string.source_data_dialog_state_success))
}