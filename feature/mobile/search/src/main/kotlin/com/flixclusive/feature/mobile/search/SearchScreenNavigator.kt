package com.flixclusive.feature.mobile.search

import com.flixclusive.core.navigation.navigator.GoBackAction
import com.flixclusive.core.navigation.navigator.ViewMediaAction
import com.flixclusive.core.navigation.navigator.ViewMediaPreviewAction

interface SearchScreenNavigator :
    GoBackAction,
    ViewMediaAction,
    ViewMediaPreviewAction
