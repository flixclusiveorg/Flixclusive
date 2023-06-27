package com.flixclusive.presentation.common

import com.flixclusive.R

data class FilmUiState(
    val isLoading: Boolean = true,
    val hasErrors: Boolean = false,
    val isFilmInWatchlist: Boolean = false,
)

enum class VideoDataDialogState(val message: UiText) {
    IDLE(message = UiText.StringValue("")),
    FETCHING(message = UiText.StringResource(R.string.video_data_dialog_state_fetching)),
    EXTRACTING(message = UiText.StringResource(R.string.video_data_dialog_state_extracting)),
    ERROR(message = UiText.StringResource(R.string.video_data_dialog_state_error)),
    UNAVAILABLE(message = UiText.StringResource(R.string.video_data_dialog_state_unavailable)),
    SUCCESS(message = UiText.StringResource(R.string.video_data_dialog_state_success));
}