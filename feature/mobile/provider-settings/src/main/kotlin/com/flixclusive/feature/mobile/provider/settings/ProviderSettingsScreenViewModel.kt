package com.flixclusive.feature.mobile.provider.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.log.warnLog
import com.flixclusive.domain.provider.usecase.get.GetProviderPluginUseCase
import com.flixclusive.provider.ProviderPlugin
import com.ramcosta.composedestinations.generated.providersettings.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ProviderSettingsScreenViewModel @Inject constructor(
    private val getProviderPlugin: GetProviderPluginUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val id = savedStateHandle.navArgs<ProviderSettingsScreenNavArgs>().id

    private val _providerPlugin = MutableStateFlow<Async<ProviderPlugin>>(Async.Loading)
    val providerPlugin = _providerPlugin.asStateFlow()

    init {
        viewModelScope.launch {
            _providerPlugin.value = try {
                val plugin = getProviderPlugin(id)
                if (plugin == null) {
                    warnLog("Provider plugin with id $id not found")
                    Async.Failure(UiText.from(R.string.provider_plugin_not_found))
                } else {
                    Async.Success(plugin)
                }
            } catch (e: Throwable) {
                errorLog("Error while trying to find SettingsScreen method for provider $id: ${e.localizedMessage}")
                Async.Failure(UiText.from(R.string.failed_to_load_provider_settings, e.message ?: "Unknown error"))
            }
        }
    }
}
