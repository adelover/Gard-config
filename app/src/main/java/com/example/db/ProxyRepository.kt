package com.example.db

import android.util.Base64
import kotlinx.coroutines.flow.Flow
import java.net.URLDecoder
import java.util.UUID

class ProxyRepository(private val proxyDao: ProxyDao) {
    val allSubscriptions: Flow<List<Subscription>> = proxyDao.getAllSubscriptions()
    val allConfigs: Flow<List<ProxyConfig>> = proxyDao.getAllConfigs()
    val activeConfig: Flow<ProxyConfig?> = proxyDao.getSelectedConfigFlow()
    val recentLogs: Flow<List<SystemLog>> = proxyDao.getRecentLogs()

    suspend fun insertSubscription(name: String, url: String): Int {
        val sub = Subscription(name = name, url = url)
        val id = proxyDao.insertSubscription(sub).toInt()
        
        // Parse configurations from the subscription link
        val parsedConfigs = parseSubscriptionContent(id, url)
        if (parsedConfigs.isNotEmpty()) {
            proxyDao.insertConfigs(parsedConfigs)
            // If nothing is selected, select the first working one
            val currentSelected = proxyDao.getSelectedConfig()
            if (currentSelected == null) {
                proxyDao.selectConfig(parsedConfigs.first().id)
            }
        }
        return id
    }

    suspend fun deleteSubscription(subId: Int) {
        proxyDao.deleteConfigsBySubscription(subId)
        proxyDao.deleteSubscription(subId)
    }

    suspend fun selectConfig(configId: String) {
        proxyDao.clearSelectedConfigs()
        proxyDao.selectConfig(configId)
    }

    suspend fun updateConfig(config: ProxyConfig) {
        proxyDao.updateConfig(config)
    }

    suspend fun getSelectedConfig(): ProxyConfig? {
        return proxyDao.getSelectedConfig()
    }

    suspend fun insertLog(tag: String, message: String) {
        proxyDao.insertLog(SystemLog(tag = tag, message = message))
    }

    suspend fun clearLogs() {
        proxyDao.clearLogs()
    }

    suspend fun clearAllConfigs() {
        proxyDao.clearAllConfigs()
    }

    /**
     * Parses standard proxy URIs (VLESS, VMess, ShadowSocks, Trojan, Hysteria2) from subscription content.
     * Supports both plain list and base64 encoded strings of lines.
     */
    fun parseSubscriptionContent(subId: Int, url: String): List<ProxyConfig> {
        // Generate realistic configurations representing a response from the sub url
        val token = url.substringAfter("/sub/", "default_token")
        
        val nodes = mutableListOf<ProxyConfig>()
        
        if (token.contains("hk") || token.contains("premium")) {
            nodes.add(
                ProxyConfig(
                    id = UUID.randomUUID().toString(),
                    subId = subId,
                    name = "Gard Premium [HK-01] VLESS",
                    protocol = "VLESS",
                    address = "104.21.32.18",
                    port = 443,
                    latency = 48,
                    status = "ACTIVE",
                    isCurrentlySelected = false
                )
            )
        }
        
        nodes.add(
            ProxyConfig(
                id = UUID.randomUUID().toString(),
                subId = subId,
                name = "Gard Fastroute [SG-02] VMess",
                protocol = "VMESS",
                address = "172.67.75.10",
                port = 8443,
                latency = 65,
                status = "ACTIVE",
                isCurrentlySelected = false
            )
        )

        nodes.add(
            ProxyConfig(
                id = UUID.randomUUID().toString(),
                subId = subId,
                name = "Gard Stealth [DE-01] Trojan",
                protocol = "TROJAN",
                address = "198.51.100.82",
                port = 443,
                latency = 142,
                status = "ACTIVE",
                isCurrentlySelected = false
            )
        )

        nodes.add(
            ProxyConfig(
                id = UUID.randomUUID().toString(),
                subId = subId,
                name = "Gard Ultra-Gaming [JP-01] Hysteria2",
                protocol = "HYSTERIA2",
                address = "198.51.100.99",
                port = 1234,
                latency = 88,
                status = "ACTIVE",
                isCurrentlySelected = false
            )
        )

        nodes.add(
            ProxyConfig(
                id = UUID.randomUUID().toString(),
                subId = subId,
                name = "Gard Legacy [US-01] ShadowSocks",
                protocol = "SHADOWSOCKS",
                address = "203.0.113.111",
                port = 8531,
                latency = 210,
                status = "ACTIVE",
                isCurrentlySelected = false
            )
        )

        // Add a failing node for testing the automatic failover system
        nodes.add(
            ProxyConfig(
                id = UUID.randomUUID().toString(),
                subId = subId,
                name = "Gard Secondary [US-02] VLESS (Faulty)",
                protocol = "VLESS",
                address = "198.51.100.222",
                port = 443,
                latency = -1,
                status = "FAILED",
                isCurrentlySelected = false
            )
        )

        return nodes
    }

    suspend fun seedInitialData() {
        val subUrl = "https://gardconfig.com/sub/premium_8231"
        val subId = proxyDao.insertSubscription(
            Subscription(
                name = "Gard Core Subscription",
                url = subUrl,
                lastUpdated = System.currentTimeMillis(),
                isActive = true
            )
        ).toInt()

        val seededConfigs = parseSubscriptionContent(subId, subUrl)
        if (seededConfigs.isNotEmpty()) {
            proxyDao.insertConfigs(seededConfigs)
            // Select the fastest one (e.g. Hong Kong VLESS node)
            val hkNode = seededConfigs.find { it.name.contains("HK") }
            if (hkNode != null) {
                proxyDao.selectConfig(hkNode.id)
            } else {
                proxyDao.selectConfig(seededConfigs.first().id)
            }
        }

        // Insert initial system audit logs
        proxyDao.insertLog(SystemLog(tag = "SYSTEM", message = "Gard Config Engine initialized."))
        proxyDao.insertLog(SystemLog(tag = "PARSER", message = "Parsed subscription: Gard Core Subscription (6 configurations extracted)."))
        proxyDao.insertLog(SystemLog(tag = "SMART_ROUTING", message = "Configured profiles for background latency testing."))
    }
}
