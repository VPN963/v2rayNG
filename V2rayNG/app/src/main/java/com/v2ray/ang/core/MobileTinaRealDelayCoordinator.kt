package com.v2ray.ang.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

/**
 * Process-local completion signal for Real Delay batches.
 * Smart Connect snapshots [generation] before starting a test and awaits the next generation,
 * avoiding races around transient UI `isTesting` state.
 */
object MobileTinaRealDelayCoordinator {
    private val _generation = MutableStateFlow(0L)
    val generation: Long
        get() = _generation.value

    fun notifyFinished() {
        _generation.update { it + 1L }
    }

    suspend fun awaitNext(afterGeneration: Long) {
        _generation.first { it > afterGeneration }
    }
}
