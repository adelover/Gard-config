package com.example.ui

import com.example.db.ProxyConfig
import com.example.db.Subscription
import com.example.db.SystemLog

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

enum class AdaptivePreset {
    STEALTH,   // Automated aggressive fragmentation and fake packet lengths to bypass DPI
    GAMING,    // Low latency routing, highly optimized UDP handling
    STANDARD   // Balanced battery and performance
}

data class ProxyUiState(
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val activePreset: AdaptivePreset = AdaptivePreset.STANDARD,
    val selectedConfig: ProxyConfig? = null,
    val configsList: List<ProxyConfig> = emptyList(),
    val subscriptionsList: List<Subscription> = emptyList(),
    val logs: List<SystemLog> = emptyList(),
    val downloadSpeedKbps: Float = 0f,
    val uploadSpeedKbps: Float = 0f,
    val totalBytesDownloaded: Long = 0L,
    val totalBytesUploaded: Long = 0L,
    val isHealthChecking: Boolean = false,
    val connectionUptimeSeconds: Long = 0L
)
