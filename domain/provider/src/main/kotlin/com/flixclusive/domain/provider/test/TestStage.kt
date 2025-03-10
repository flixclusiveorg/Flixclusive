package com.flixclusive.domain.provider.test

import android.content.Context
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.core.locale.R as LocaleR


/**
 *
 * The test stages of a provider.
 *
 * */
sealed class TestStage(val providerOnTest: ProviderMetadata? = null) {
    protected abstract val index: Int

    class Idle(providerOnTest: ProviderMetadata?) : TestStage(providerOnTest) {
        override val index: Int get() = 0

        companion object {
            val TestStage.isIdle: Boolean
                get() = this is Idle
        }
    }
    class Stage1(providerOnTest: ProviderMetadata?) : TestStage(providerOnTest) {
        override val index: Int get() = 1
    }
    class Stage2(providerOnTest: ProviderMetadata?) : TestStage(providerOnTest) {
        override val index: Int get() = 2
    }
    class Done(providerOnTest: ProviderMetadata?) : TestStage(providerOnTest) {
        override val index: Int get() = 3
    }

    fun toString(context: Context): String {
        return when (this) {
            is Idle -> ""
            is Stage1 -> context.getString(LocaleR.string.provider_test_stage_stage1)
            is Stage2 -> context.getString(LocaleR.string.provider_test_stage_stage2)
            is Done -> context.getString(LocaleR.string.provider_test_stage_done)
        }
    }

    operator fun compareTo(other: TestStage): Int {
        return when {
            index != other.index -> index - other.index
            else -> compareProviders(
                oldProvider = providerOnTest,
                newProvider = other.providerOnTest
            )
        }
    }

    private fun compareProviders(
        oldProvider: ProviderMetadata?,
        newProvider: ProviderMetadata?
    ): Int {
        return when {
            oldProvider == null && newProvider == null -> 0
            oldProvider == null -> -1
            newProvider == null -> 1
            else -> oldProvider.id!!.compareTo(newProvider.id!!)
        }
    }
}