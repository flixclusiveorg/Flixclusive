package com.flixclusive.domain.provider.usecase.get

import com.flixclusive.core.network.util.Resource
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.Repository


/*
 * TODO: Improve this. It can literally
 *  just use a new `buildsBranch` property then pass that on
 *  branch parameter in `getRawLink`
 * */


/**
 * Use case to retrieve a online provider/s from an 'updater.json' of 'builds' branch
 */
interface GetProviderFromRemoteUseCase {
    /**
     * Retrieves a list of online [ProviderMetadata]s from the specified repository.
     * */
    suspend operator fun invoke(repository: Repository): Resource<List<ProviderMetadata>>

    /**
     * Retrieves a single online [ProviderMetadata] from the specified repository.
     * */
    suspend operator fun invoke(repository: Repository, id: String): Resource<ProviderMetadata>
}
