package com.flixclusive.core.navigation.navargs

import java.io.Serializable

data class PinWithHintResult(
    val pin: String?,
    val pinHint: String?,
) : Serializable
