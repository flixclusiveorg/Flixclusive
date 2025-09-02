package com.flixclusive.core.ui.common.navigation.navargs

import java.io.Serializable

data class PinWithHintResult(
    val pin: String?,
    val pinHint: String?,
) : Serializable
