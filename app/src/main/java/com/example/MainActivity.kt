package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.DashboardScreen
import com.example.ui.ProxyViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private val viewModel: ProxyViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val uiState by viewModel.uiState.collectAsState()
        val subscriptions by viewModel.subscriptions.collectAsState(initial = emptyList())
        val configs by viewModel.configs.collectAsState(initial = emptyList())
        val logs by viewModel.logs.collectAsState(initial = emptyList())

        DashboardScreen(
          uiState = uiState,
          subscriptions = subscriptions,
          configs = configs,
          logs = logs,
          onToggleConnect = { viewModel.toggleConnection() },
          onSelectPreset = { preset -> viewModel.selectPreset(preset) },
          onAddSub = { name, url -> viewModel.addSubscription(name, url) },
          onRemoveSub = { id -> viewModel.removeSubscription(id) },
          onSelectConfig = { configId -> viewModel.selectConfig(configId) },
          onClearLogs = { viewModel.clearAllLogs() },
          onTriggerHealthCheck = { viewModel.runManualHealthCheck() },
          onAddManualConfig = { config -> viewModel.addManualConfig(config) }
        )
      }
    }
  }
}

