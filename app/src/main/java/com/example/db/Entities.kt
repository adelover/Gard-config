package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val url: String,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

@Entity(tableName = "proxy_configs")
data class ProxyConfig(
    @PrimaryKey val id: String,
    val subId: Int,
    val name: String,
    val protocol: String, // VLESS, VMess, ShadowSocks, Trojan, Hysteria2
    val address: String,
    val port: Int,
    val latency: Int = -1, // in milliseconds, -1 means failed
    val status: String = "PENDING", // PENDING, ACTIVE, FAILED
    val isCurrentlySelected: Boolean = false,
    val speedKbps: Float = 0f
)

@Entity(tableName = "system_logs")
data class SystemLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String,
    val message: String
)
