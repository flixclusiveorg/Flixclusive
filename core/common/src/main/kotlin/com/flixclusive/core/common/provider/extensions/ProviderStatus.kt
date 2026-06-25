package com.flixclusive.core.common.provider.extensions

import androidx.compose.ui.graphics.Color
import com.flixclusive.model.provider.ProviderStatus

fun ProviderStatus.asStatusColor() = when (this) {
    ProviderStatus.Down -> Color(0xFFFF3030)
    ProviderStatus.Beta -> Color(0xFF00C4FF)
    ProviderStatus.Working -> Color(0xFF00FF04)
}
