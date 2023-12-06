package com.flixclusive.data.usecase

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.flixclusive.domain.model.provider.SourceProviderDetails
import com.flixclusive.domain.repository.FilmSourcesRepository
import com.flixclusive.domain.usecase.ModifyProvidersUseCase
import javax.inject.Inject

class ModifyProvidersUseCaseImpl @Inject constructor(
    private val filmSourcesRepository: FilmSourcesRepository
) : ModifyProvidersUseCase {
    override val availableProviders: SnapshotStateList<SourceProviderDetails>
        get() = filmSourcesRepository.providers

    override fun swap(fromIndex: Int, toIndex: Int) {
        val size = filmSourcesRepository.providers.size
        if (fromIndex == toIndex || fromIndex < 0 || toIndex < 0 || fromIndex >= size || toIndex >= size) {
            return
        }
        
        val temp = filmSourcesRepository.providers[fromIndex]
        filmSourcesRepository.providers[fromIndex] = filmSourcesRepository.providers[toIndex]
        filmSourcesRepository.providers[toIndex] = temp
    }

    override fun toggleUsage(index: Int) {
        val data = filmSourcesRepository.providers[index]
        filmSourcesRepository.providers[index] = data.copy(
            isIgnored = !data.isIgnored
        )
    }
}