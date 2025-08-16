package com.flixclusive.core.ui.common.navigation.navargs

import com.flixclusive.core.database.entity.user.User
import java.io.Serializable

data class PinVerificationResult(
    val user: User,
    val isVerified: Boolean,
) : Serializable
