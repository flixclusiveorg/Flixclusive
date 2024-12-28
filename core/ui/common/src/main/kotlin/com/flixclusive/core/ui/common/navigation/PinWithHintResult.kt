package com.flixclusive.core.ui.common.navigation

import java.io.Serializable

data class PinWithHintResult(
    val pin: String?,
    val pinHint: String?,
) : Serializable