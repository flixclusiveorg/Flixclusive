package com.flixclusive.core.ui.player

enum class PlayerProviderState {
    LOADING,
    SELECTED
}


data class PlayerUiState(
    val selectedSourceLink: Int = 0,
    val selectedProvider: String? = null,
    val selectedProviderState: PlayerProviderState = PlayerProviderState.SELECTED,
    val selectedResizeMode: Int = 0,
    val lastOpenedPanel: Int = 0
)
