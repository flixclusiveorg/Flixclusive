package com.flixclusive.feature.mobile.user.edit

import com.flixclusive.core.navigation.navigator.NavigateBack
import com.flixclusive.core.navigation.navigator.NavigateToChooseProfileScreen
import com.flixclusive.core.navigation.navigator.NavigateToOpenPinScreen
import com.flixclusive.core.navigation.navigator.NavigateToSelectAvatarScreen

interface NavigatorUserEditScreen :
    NavigateToOpenPinScreen,
    NavigateToSelectAvatarScreen,
    NavigateToChooseProfileScreen,
    NavigateBack
