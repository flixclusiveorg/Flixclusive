package com.flixclusive.data.provider.util.extensions

import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.Status

val ProviderMetadata.isNotUsable: Boolean
    get() = status == Status.Down || status == Status.Maintenance
