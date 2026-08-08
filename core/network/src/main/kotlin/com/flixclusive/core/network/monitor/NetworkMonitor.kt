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
}
