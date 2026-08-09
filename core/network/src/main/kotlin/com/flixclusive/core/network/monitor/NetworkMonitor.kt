package com.flixclusive.core.network.monitor

import kotlinx.coroutines.flow.Flow

interface NetworkMonitor {
    val isOnline: Flow<Boolean>

    /**
     * Whether the connection currently carrying traffic is one the user likely pays per byte for —
     * mobile data, or a hotspot the owner has flagged as metered. Emits `true` when there is no
     * usable connection to inspect, so a caller gating expensive work errs towards holding off
     * rather than towards spending someone's data allowance.
     */
    val isMetered: Flow<Boolean>

    /**
     * [isMetered] answered right now, read straight from the system rather than from the flow.
     *
     * [isMetered] is shared with `WhileSubscribed`, so its replay cache keeps the last value seen
     * while something was collecting and the upstream stops in between. A one-shot `first()` on it
     * therefore returns whatever was true the last time anyone looked — which for a caller deciding
     * "may I spend data right now?" is exactly the wrong answer after the user has switched
     * networks. Use this for that decision and the flow for anything that observes over time.
     */
    fun isMeteredNow(): Boolean

    /** [isOnline] answered right now, read straight from the system — same reasoning as [isMeteredNow]. */
    fun isOnlineNow(): Boolean
}
