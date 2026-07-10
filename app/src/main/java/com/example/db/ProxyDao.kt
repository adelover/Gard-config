package com.example.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProxyDao {
    // Subscriptions
    @Query("SELECT * FROM subscriptions ORDER BY id DESC")
    fun getAllSubscriptions(): Flow<List<Subscription>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: Subscription): Long

    @Query("DELETE FROM subscriptions WHERE id = :subId")
    suspend fun deleteSubscription(subId: Int)

    // Proxy Configurations
    @Query("SELECT * FROM proxy_configs ORDER BY latency ASC")
    fun getAllConfigs(): Flow<List<ProxyConfig>>

    @Query("SELECT * FROM proxy_configs WHERE subId = :subId")
    suspend fun getConfigsForSubscription(subId: Int): List<ProxyConfig>

    @Query("SELECT * FROM proxy_configs WHERE id = :configId LIMIT 1")
    suspend fun getConfigById(configId: String): ProxyConfig?

    @Query("SELECT * FROM proxy_configs WHERE isCurrentlySelected = 1 LIMIT 1")
    fun getSelectedConfigFlow(): Flow<ProxyConfig?>

    @Query("SELECT * FROM proxy_configs WHERE isCurrentlySelected = 1 LIMIT 1")
    suspend fun getSelectedConfig(): ProxyConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfigs(configs: List<ProxyConfig>)

    @Update
    suspend fun updateConfig(config: ProxyConfig)

    @Query("UPDATE proxy_configs SET isCurrentlySelected = 0")
    suspend fun clearSelectedConfigs()

    @Query("UPDATE proxy_configs SET isCurrentlySelected = 1 WHERE id = :configId")
    suspend fun selectConfig(configId: String)

    @Query("DELETE FROM proxy_configs WHERE subId = :subId")
    suspend fun deleteConfigsBySubscription(subId: Int)

    @Query("DELETE FROM proxy_configs")
    suspend fun clearAllConfigs()

    // Logs
    @Query("SELECT * FROM system_logs ORDER BY timestamp DESC LIMIT 150")
    fun getRecentLogs(): Flow<List<SystemLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SystemLog)

    @Query("DELETE FROM system_logs")
    suspend fun clearLogs()
}
