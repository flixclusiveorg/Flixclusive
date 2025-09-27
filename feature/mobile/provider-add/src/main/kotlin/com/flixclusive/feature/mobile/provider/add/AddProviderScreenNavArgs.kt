package com.flixclusive.feature.mobile.provider.add

import com.flixclusive.model.provider.Repository

data class AddProviderScreenNavArgs(
    /**
     * Initial repository to be selected in the repository filter.
     * */
    val initialSelectedRepositoryFilter: Repository? = null,
)
