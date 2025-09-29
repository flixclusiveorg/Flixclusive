package com.flixclusive.feature.mobile.user.add

import com.flixclusive.core.navigation.navigator.GoBackAction
import com.flixclusive.core.navigation.navigator.OpenPinScreenAction
import com.flixclusive.core.navigation.navigator.SelectAvatarAction
import com.flixclusive.core.navigation.navigator.StartHomeScreenAction

interface AddUserScreenNavigator :
    GoBackAction,
    StartHomeScreenAction,
    OpenPinScreenAction,
    SelectAvatarAction
