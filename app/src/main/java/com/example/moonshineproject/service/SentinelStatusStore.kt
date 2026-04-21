package com.example.moonshineproject.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class SentinelPhase {
    INACTIVE,
    WATCHING_AWAKE,
    SLEEP_DETECTED,
    MONITORING_SLEEP
}

data class SentinelUiStatus(
    val phase: SentinelPhase = SentinelPhase.INACTIVE,
    val isServiceRunning: Boolean = false,
    val isSleepDetected: Boolean = false,
    val statusMessage: String = "Sentinel inativo"
)

object SentinelStatusStore {
    private val _status = MutableStateFlow(SentinelUiStatus())
    val status: StateFlow<SentinelUiStatus> = _status

    fun update(status: SentinelUiStatus) {
        _status.value = status
    }

    fun reset() {
        _status.value = SentinelUiStatus()
    }
}
