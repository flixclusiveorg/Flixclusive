package com.flixclusive.feature.mobile.user.add

import com.flixclusive.core.navigation.navigator.NavigateBack
import com.flixclusive.core.navigation.navigator.NavigateToHomeScreen
import com.flixclusive.core.navigation.navigator.NavigateToOpenPinScreen
import com.flixclusive.core.navigation.navigator.NavigateToSelectAvatarScreen

interface NavigatorAddUserScreenNavigateTo :
    NavigateBack,
    NavigateToHomeScreen,
    NavigateToOpenPinScreen,
    NavigateToSelectAvatarScreen
