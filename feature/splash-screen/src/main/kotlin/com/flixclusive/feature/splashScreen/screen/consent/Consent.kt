package com.flixclusive.feature.splashScreen.screen.consent

import androidx.compose.runtime.Stable
import com.flixclusive.feature.splashScreen.OnBoardingGuide

@Stable
internal data class Consent(
    override val title: String,
    override val description: String,
    val optInMessage: String? = null
) : OnBoardingGuide