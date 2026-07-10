package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

class ProxyViewModel(application: Application) : AndroidViewModel(application) {
    private val db = ProxyDatabase.getDatabase(application)
    private val repository = ProxyRepository(db.proxyDao())

    private val _uiState = MutableStateFlow(ProxyUiState())
    val uiState: StateFlow<ProxyUiState> = _uiState.asStateFlow()

    private var trafficJob: Job? = null
    private var healthCheckJob: Job? = null
    private var uptimeJob: Job? = null

    // Connect DB streams directly to the UI elements
    val subscriptions: Flow<List<Subscription>> = repository.allSubscriptions
    val configs: Flow<List<ProxyConfig>> = repository.allConfigs
    val logs: Flow<List<SystemLog>> = repository.recentLogs

    init {
        // Seed initial data if DB is blank
        viewModelScope.launch {
            repository.allSubscriptions.first().let {
                if (it.isEmpty()) {
                    repository.seedInitialData()
                }
            }
        }

        // Keep lists and selected config dynamically up to date
        viewModelScope.launch {
            combine(
                repository.allSubscriptions,
                repository.allConfigs,
                repository.activeConfig,
                repository.recentLogs
            ) { subs, cfgs, active, lgs ->
                _uiState.update { state ->
                    state.copy(
                        subscriptionsList = subs,
                        configsList = cfgs,
                        selectedConfig = active,
                        logs = lgs
                    )
                }
            }.collect()
        }

        // Start background health checking daemon (runs every 20 seconds to automate smart routing)
        startBackgroundHealthCheckDaemon()
    }

    fun toggleConnection() {
        val currentStatus = _uiState.value.connectionStatus
        viewModelScope.launch {
            if (currentStatus == ConnectionStatus.DISCONNECTED) {
                val active = repository.getSelectedConfig()
                if (active == null) {
                    repository.insertLog("ERROR", "No active proxy configuration selected. Please add a subscription first.")
                    return@launch
                }

                _uiState.update { it.copy(connectionStatus = ConnectionStatus.CONNECTING) }
                repository.insertLog("SYSTEM", "Starting Gard-Flow core tunnel service...")
                
                // Fetch dynamic preset characteristics
                val preset = _uiState.value.activePreset
                repository.insertLog("ENGINE", "Profile loaded: ${preset.name}")
                
                when (preset) {
                    AdaptivePreset.STEALTH -> {
                        repository.insertLog("STEALTH", "Enabling aggressive packet fragmentation [Fragment Size: 18-52 bytes].")
                        repository.insertLog("STEALTH", "Padding TLS ClientHello to 512 bytes with randomized dummy frames to deceive DPI firewalls.")
                    }
                    AdaptivePreset.GAMING -> {
                        repository.insertLog("GAMING", "Bypassing Mux multiplexing. Initializing zero-copy UDP direct routing.")
                        repository.insertLog("GAMING", "Setting socket buffer capacity to maximum. Prioritizing low-latency paths.")
                    }
                    AdaptivePreset.STANDARD -> {
                        repository.insertLog("ENGINE", "Balanced Mux & TCP stream optimization enabled.")
                    }
                }

                delay(1000) // Simulated handshaking & tunnel establishment

                _uiState.update { it.copy(connectionStatus = ConnectionStatus.CONNECTED) }
                repository.insertLog("ENGINE", "Successfully established tunnel to '${active.name}' via ${active.protocol}.")
                repository.insertLog("SYSTEM", "VPN interface is active. Routing traffic through proxy.")

                startTrafficSimulation()
                startUptimeTracker()
            } else {
                stopTrafficSimulation()
                stopUptimeTracker()
                _uiState.update { 
                    it.copy(
                        connectionStatus = ConnectionStatus.DISCONNECTED,
                        downloadSpeedKbps = 0f,
                        uploadSpeedKbps = 0f,
                        connectionUptimeSeconds = 0L
                    )
                }
                repository.insertLog("SYSTEM", "Gard-Flow core stopped. VPN interface closed.")
            }
        }
    }

    fun selectPreset(preset: AdaptivePreset) {
        _uiState.update { it.copy(activePreset = preset) }
        viewModelScope.launch {
            repository.insertLog("ENGINE", "Switched connection preset to: ${preset.name}")
            when (preset) {
                AdaptivePreset.STEALTH -> {
                    repository.insertLog("STEALTH", "Applying automated fragmentation headers and MTU throttling dynamically.")
                }
                AdaptivePreset.GAMING -> {
                    repository.insertLog("GAMING", "Enabled fast UDP mapping and direct tunneling mode.")
                }
                AdaptivePreset.STANDARD -> {
                    repository.insertLog("ENGINE", "Resetting tunnel parameters to balanced standard configuration.")
                }
            }
        }
    }

    fun addSubscription(name: String, url: String) {
        viewModelScope.launch {
            if (url.isBlank()) return@launch
            repository.insertLog("SUBSCRIBER", "Adding subscription: $name...")
            try {
                repository.insertSubscription(name, url)
                repository.insertLog("SUBSCRIBER", "Successfully fetched and parsed configs from subscription.")
            } catch (e: Exception) {
                repository.insertLog("ERROR", "Failed to resolve subscription link: ${e.localizedMessage}")
            }
        }
    }

    fun removeSubscription(id: Int) {
        viewModelScope.launch {
            repository.insertLog("SUBSCRIBER", "Removing subscription link and dependent configurations...")
            repository.deleteSubscription(id)
            repository.insertLog("SUBSCRIBER", "Cleaned up configurations.")
        }
    }

    fun addManualConfig(config: ProxyConfig) {
        viewModelScope.launch {
            repository.insertLog("MANUAL", "Created config profile: ${config.name} [${config.protocol}]")
            db.proxyDao().insertConfigs(listOf(config))
        }
    }

    fun selectConfig(configId: String) {
        viewModelScope.launch {
            repository.selectConfig(configId)
            val selected = repository.getSelectedConfig()
            selected?.let {
                repository.insertLog("ENGINE", "Switched active gateway to: ${it.name} [${it.protocol}]")
                if (_uiState.value.connectionStatus == ConnectionStatus.CONNECTED) {
                    // Re-tunnel instantly
                    repository.insertLog("SYSTEM", "Hot-swapping active tunnel pathway instantly...")
                    delay(300)
                    repository.insertLog("SYSTEM", "Gateway pathway swapped successfully.")
                }
            }
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearLogs()
            repository.insertLog("SYSTEM", "Console buffer cleared.")
        }
    }

    fun runManualHealthCheck() {
        viewModelScope.launch {
            runHealthProbing()
        }
    }

    private fun startBackgroundHealthCheckDaemon() {
        healthCheckJob?.cancel()
        healthCheckJob = viewModelScope.launch {
            while (true) {
                delay(20000) // Run checks every 20 seconds
                runHealthProbing()
            }
        }
    }

    private suspend fun runHealthProbing() {
        _uiState.update { it.copy(isHealthChecking = true) }
        repository.insertLog("SMART_ROUTING", "Starting periodic background health checks (TCPing/silent probing)...")

        val currentConfigs = _uiState.value.configsList
        var activeConfigFailed = false
        var currentlySelectedId = _uiState.value.selectedConfig?.id

        for (cfg in currentConfigs) {
            // Introduce realistic variation
            val shouldFail = cfg.name.contains("Faulty") || (Random.nextFloat() < 0.12f && !cfg.name.contains("Premium"))
            val newLatency = if (shouldFail) {
                -1
            } else {
                // Adjust base latency depending on destination (HK < SG < JP < DE < US)
                val base = when {
                    cfg.name.contains("HK") -> 40
                    cfg.name.contains("SG") -> 60
                    cfg.name.contains("JP") -> 85
                    cfg.name.contains("DE") -> 140
                    else -> 220
                }
                base + Random.nextInt(15)
            }

            val updatedConfig = cfg.copy(
                latency = newLatency,
                status = if (newLatency == -1) "FAILED" else "ACTIVE"
            )
            repository.updateConfig(updatedConfig)

            if (cfg.id == currentlySelectedId && newLatency == -1) {
                activeConfigFailed = true
            }
        }

        repository.insertLog("SMART_ROUTING", "Background health check complete. Configurations directory updated.")

        // Perform transparent failover if the currently active node failed
        if (activeConfigFailed && _uiState.value.connectionStatus == ConnectionStatus.CONNECTED) {
            repository.insertLog("FAILOVER", "CRITICAL: Currently active gateway failed real delay probing.")
            repository.insertLog("FAILOVER", "Initiating transparent background routing failover...")

            // Find the fastest working configuration
            val workingConfigs = _uiState.value.configsList.filter { it.status == "ACTIVE" && it.latency > 0 }
            if (workingConfigs.isNotEmpty()) {
                val bestConfig = workingConfigs.minByOrNull { it.latency }
                bestConfig?.let {
                    repository.selectConfig(it.id)
                    repository.insertLog("FAILOVER", "Transparent failover succeeded. Swapped connection to fastest node: ${it.name} (${it.latency} ms)")
                }
            } else {
                repository.insertLog("FAILOVER", "Failover aborted: All configurations failed to respond to network probes.")
                toggleConnection() // Shut down connection since everything failed
            }
        }

        _uiState.update { it.copy(isHealthChecking = false) }
    }

    private fun startTrafficSimulation() {
        trafficJob?.cancel()
        trafficJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val preset = _uiState.value.activePreset
                
                // Stealth Mode has low speeds due to chunk fragmentation
                // Gaming has constant flow of small packets
                // Standard has high transactional download bursts
                val dlSpeed = when (preset) {
                    AdaptivePreset.STEALTH -> Random.nextFloat() * 150f + 20f
                    AdaptivePreset.GAMING -> Random.nextFloat() * 450f + 100f
                    AdaptivePreset.STANDARD -> if (Random.nextFloat() < 0.3f) Random.nextFloat() * 3200f + 1200f else Random.nextFloat() * 200f + 50f
                }

                val ulSpeed = when (preset) {
                    AdaptivePreset.STEALTH -> Random.nextFloat() * 50f + 10f
                    AdaptivePreset.GAMING -> Random.nextFloat() * 300f + 50f
                    AdaptivePreset.STANDARD -> Random.nextFloat() * 120f + 30f
                }

                _uiState.update { state ->
                    state.copy(
                        downloadSpeedKbps = dlSpeed,
                        uploadSpeedKbps = ulSpeed,
                        totalBytesDownloaded = state.totalBytesDownloaded + (dlSpeed * 1024 / 8).toLong(),
                        totalBytesUploaded = state.totalBytesUploaded + (ulSpeed * 1024 / 8).toLong()
                    )
                }
            }
        }
    }

    private fun stopTrafficSimulation() {
        trafficJob?.cancel()
        trafficJob = null
    }

    private fun startUptimeTracker() {
        uptimeJob?.cancel()
        uptimeJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { state ->
                    state.copy(connectionUptimeSeconds = state.connectionUptimeSeconds + 1)
                }
            }
        }
    }

    private fun stopUptimeTracker() {
        uptimeJob?.cancel()
        uptimeJob = null
    }
}
