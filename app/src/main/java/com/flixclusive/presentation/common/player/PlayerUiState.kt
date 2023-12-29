package com.flixclusive.presentation.common.player

import com.flixclusive.domain.common.Resource

data class PlayerUiState(
    val selectedSourceLink: Int = 0,
    val selectedProvider: String? = null,
    val selectedProviderState: Resource<Any?> = Resource.Success(null),
    val selectedResizeMode: Int = 0,
    val lastOpenedPanel: Int = 0,
    val screenBrightness: Float = 1F,
    val volume: Float = 0F
)
