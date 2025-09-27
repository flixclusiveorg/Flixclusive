package com.flixclusive.domain.provider.testing

import android.content.Context
import com.flixclusive.domain.provider.R
import com.flixclusive.model.provider.ProviderMetadata

/**
 *
 * The test stages of a provider.
 *
 * */
sealed class TestStage(
    val providerOnTest: ProviderMetadata? = null,
) {
    protected abstract val index: Int

    val isIdle: Boolean get() = this is Idle

    data object Idle : TestStage(null) {
        override val index: Int get() = 0
    }

    class Stage1(
        providerOnTest: ProviderMetadata?,
    ) : TestStage(providerOnTest) {
        override val index: Int get() = 1
    }

    class Stage2(
        providerOnTest: ProviderMetadata?,
    ) : TestStage(providerOnTest) {
        override val index: Int get() = 2
    }

    fun toString(context: Context): String {
        return when (this) {
            is Idle -> context.getString(R.string.provider_test_stage_idle)
            is Stage1 -> context.getString(R.string.provider_test_stage_stage1)
            is Stage2 -> context.getString(R.string.provider_test_stage_stage2)
        }
    }

    operator fun compareTo(other: TestStage): Int {
        return when {
            index != other.index -> index - other.index
            else -> compareProviders(
                oldProvider = providerOnTest,
                newProvider = other.providerOnTest,
            )
        }
    }

    private fun compareProviders(
        oldProvider: ProviderMetadata?,
        newProvider: ProviderMetadata?,
    ): Int {
        return when {
            oldProvider == null && newProvider == null -> 0
            oldProvider == null -> -1
            newProvider == null -> 1
            else -> oldProvider.id.compareTo(newProvider.id)
        }
    }
}
