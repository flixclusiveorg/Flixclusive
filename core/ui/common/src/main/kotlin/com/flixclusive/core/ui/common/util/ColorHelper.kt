package com.flixclusive.core.ui.common.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.flixclusive.model.provider.Status

@Composable
fun Color.onMediumEmphasis(
    emphasis: Float = 0.6F
) = copy(alpha = emphasis)


fun getProviderStatusColor(status: Status)
    = when (status) {
        Status.Down -> Color(0xFFFF3030)
        Status.Maintenance -> Color(0xFFFFBF1B)
        Status.Beta -> Color(0xFF00C4FF)
        Status.Working -> Color(0xFF00FF04)
    }