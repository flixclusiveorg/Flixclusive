package com.flixclusive.presentation.common.player

import com.flixclusive.domain.common.Resource

data class PlayerUiState(
    val selectedServer: Int = 0,
    val selectedSource: String? = null,
    val selectedSourceState: Resource<Any?> = Resource.Success(null),
    val selectedResizeMode: Int = 0,
    val lastOpenedPanel: Int = 0,
    val screenBrightness: Float = 1F,
    val volume: Float = 0F
)
