package com.flixclusive.core.common.provider

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import com.flixclusive.model.provider.ProviderStatus

@Stable
fun getProviderStatusContainerColor(status: ProviderStatus) =
    when (status) {
        ProviderStatus.Down -> Color(0xFFFF3030)
        ProviderStatus.Beta -> Color(0xFF00C4FF)
        ProviderStatus.Working -> Color(0xFF00FF04)
    }
