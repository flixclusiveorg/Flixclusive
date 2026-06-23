package com.flixclusive.core.presentation.common.util

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner

object ViewModelUtil {
    /**
     * Scopes the [hiltViewModel] to the activity scope.
     *
     * This avoids re-creating the [ViewModel] for screens that
     * could have long loading states and eases up user experience.
     * */
    @Composable
    inline fun <reified VM : ViewModel> activityHiltViewModel(
        viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalActivity.current as ComponentActivity) {
            "LocalActivity has not been provided yet"
        },
        key: String? = null,
    ): VM = hiltViewModel(viewModelStoreOwner, key)
}
