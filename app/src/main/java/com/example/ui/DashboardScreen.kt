package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.db.ProxyConfig
import com.example.db.Subscription
import com.example.db.SystemLog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: ProxyUiState,
    subscriptions: List<Subscription>,
    configs: List<ProxyConfig>,
    logs: List<SystemLog>,
    onToggleConnect: () -> Unit,
    onSelectPreset: (AdaptivePreset) -> Unit,
    onAddSub: (String, String) -> Unit,
    onRemoveSub: (Int) -> Unit,
    onSelectConfig: (String) -> Unit,
    onClearLogs: () -> Unit,
    onTriggerHealthCheck: () -> Unit,
    onAddManualConfig: (ProxyConfig) -> Unit
) {
    var activeTab by remember { mutableStateOf(0) } // 0: Connect, 1: Nodes, 2: Subscriptions, 3: Console
    var showingAddSubDialog by remember { mutableStateOf(false) }
    var showingProtocolBottomSheet by remember { mutableStateOf(false) }
    var activeManualBuilderType by remember { mutableStateOf<String?>(null) } // "VLESS", "VMESS", "TROJAN", etc.

    val darkBackground = Color(0xFF0F172A) // Cosmic Slate Theme
    val accentGreen = Color(0xFF10B981) // Emerald Green Accent
    val accentNavy = Color(0xFF1E293B)
    val cardBackground = Color(0xFF1E293B)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "Gard Security Engine",
                            tint = if (uiState.connectionStatus == ConnectionStatus.CONNECTED) accentGreen else Color.Gray,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Gard Config",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 1.2.sp
                            )
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // Active Config Status Badge
                        uiState.selectedConfig?.let {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF0F172A).copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(
                                                if (uiState.connectionStatus == ConnectionStatus.CONNECTED) accentGreen else Color.LightGray,
                                                CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = it.protocol,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = darkBackground,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = darkBackground,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.PowerSettingsNew, contentDescription = "Tunnel Control") },
                    label = { Text("Power") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = accentGreen,
                        selectedTextColor = accentGreen,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = accentNavy
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.Speed, contentDescription = "Nodes Directory") },
                    label = { Text("Gateways") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = accentGreen,
                        selectedTextColor = accentGreen,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = accentNavy
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Default.CloudSync, contentDescription = "Sub Links manager") },
                    label = { Text("Subscriptions") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = accentGreen,
                        selectedTextColor = accentGreen,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = accentNavy
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    icon = { Icon(Icons.Default.Terminal, contentDescription = "System log console") },
                    label = { Text("Console") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = accentGreen,
                        selectedTextColor = accentGreen,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = accentNavy
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showingProtocolBottomSheet = true },
                containerColor = accentGreen,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Proxy Protocol Menu"
                )
            }
        },
        containerColor = darkBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(darkBackground)
        ) {
            when (activeTab) {
                0 -> ConnectTab(
                    uiState = uiState,
                    onToggleConnect = onToggleConnect,
                    onSelectPreset = onSelectPreset
                )
                1 -> GatewaysTab(
                    configs = configs,
                    selectedConfig = uiState.selectedConfig,
                    onSelectConfig = onSelectConfig,
                    isHealthChecking = uiState.isHealthChecking,
                    onTriggerHealthCheck = onTriggerHealthCheck
                )
                2 -> SubscriptionsTab(
                    subscriptions = subscriptions,
                    onRemoveSub = onRemoveSub,
                    onShowAddDialog = { showingAddSubDialog = true }
                )
                3 -> ConsoleTab(
                    logs = logs,
                    onClearLogs = onClearLogs
                )
            }

            if (showingAddSubDialog) {
                AddSubscriptionDialog(
                    onDismiss = { showingAddSubDialog = false },
                    onConfirm = { name, url ->
                        onAddSub(name, url)
                        showingAddSubDialog = false
                    }
                )
            }

            if (showingProtocolBottomSheet) {
                ProtocolSelectorBottomSheet(
                    onDismiss = { showingProtocolBottomSheet = false },
                    onSelectOption = { option ->
                        showingProtocolBottomSheet = false
                        if (option == "SUBLINK") {
                            showingAddSubDialog = true
                        } else {
                            activeManualBuilderType = option
                        }
                    }
                )
            }

            activeManualBuilderType?.let { builderType ->
                ProtocolManualBuilderDialog(
                    protocolType = builderType,
                    configsList = configs,
                    onDismiss = { activeManualBuilderType = null },
                    onConfirm = { config ->
                        onAddManualConfig(config)
                        activeManualBuilderType = null
                    }
                )
            }
        }
    }
}

@Composable
fun ConnectTab(
    uiState: ProxyUiState,
    onToggleConnect: () -> Unit,
    onSelectPreset: (AdaptivePreset) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            GardShieldButton(
                status = uiState.connectionStatus,
                onClick = onToggleConnect
            )
        }

        // Active Connection Info Card
        item {
            AnimatedVisibility(
                visible = uiState.connectionStatus == ConnectionStatus.CONNECTED,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                uiState.selectedConfig?.let { active ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color(0xFF10B981).copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Dns,
                                        contentDescription = "Active gateway",
                                        tint = Color(0xFF10B981)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = active.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${active.address}:${active.port} • ${active.protocol}",
                                        color = Color.LightGray,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Uptime Counter
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Uptime", color = Color.Gray, fontSize = 9.sp)
                                val m = uiState.connectionUptimeSeconds / 60
                                val s = uiState.connectionUptimeSeconds % 60
                                Text(
                                    text = String.format("%02d:%02d", m, s),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Real-time Traffic throughput
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Realtime Bandwidth & Telemetry",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TelemetryDashboardPill(
                            label = "Download Speed",
                            value = if (uiState.connectionStatus == ConnectionStatus.CONNECTED) {
                                String.format("%.1f KB/s", uiState.downloadSpeedKbps)
                            } else {
                                "0.0 KB/s"
                            },
                            subtext = "Received: ${formatBytes(uiState.totalBytesDownloaded)}",
                            icon = Icons.Default.ArrowDownward,
                            color = Color(0xFF10B981)
                        )
                        TelemetryDashboardPill(
                            label = "Upload Speed",
                            value = if (uiState.connectionStatus == ConnectionStatus.CONNECTED) {
                                String.format("%.1f KB/s", uiState.uploadSpeedKbps)
                            } else {
                                "0.0 KB/s"
                            },
                            subtext = "Sent: ${formatBytes(uiState.totalBytesUploaded)}",
                            icon = Icons.Default.ArrowUpward,
                            color = Color(0xFF3B82F6)
                        )
                    }
                }
            }
        }

        // Adaptive Profiles Presets Replacing Fragment/Mux settings
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Adaptive Optimization Presets",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Abstracts packet size fragmentation and socket optimizations into smart presets.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    PresetCardSelection(
                        selected = uiState.activePreset,
                        onSelected = onSelectPreset
                    )
                }
            }
        }
    }
}

@Composable
fun GardShieldButton(
    status: ConnectionStatus,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val colorScheme = when (status) {
        ConnectionStatus.CONNECTED -> Color(0xFF10B981)
        ConnectionStatus.CONNECTING -> Color(0xFFF59E0B)
        ConnectionStatus.DISCONNECTED -> Color(0xFF475569)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(200.dp)
            .clickable(onClick = onClick)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (status != ConnectionStatus.DISCONNECTED) {
                drawCircle(
                    color = colorScheme.copy(alpha = 0.12f),
                    radius = (size.minDimension / 2) * scale,
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            drawCircle(
                color = colorScheme,
                radius = (size.minDimension / 2.3f),
                style = Stroke(width = 3.dp.toPx())
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E293B))
        ) {
            Icon(
                imageVector = when (status) {
                    ConnectionStatus.CONNECTED -> Icons.Default.Shield
                    ConnectionStatus.CONNECTING -> Icons.Default.ChangeCircle
                    ConnectionStatus.DISCONNECTED -> Icons.Default.ShieldMoon
                },
                contentDescription = "Shield State",
                tint = colorScheme,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = when (status) {
                    ConnectionStatus.CONNECTED -> "SECURE"
                    ConnectionStatus.CONNECTING -> "ROUTING"
                    ConnectionStatus.DISCONNECTED -> "BYPASS OFF"
                },
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = when (status) {
                    ConnectionStatus.CONNECTED -> "Tunnel established"
                    ConnectionStatus.CONNECTING -> "Handshaking..."
                    ConnectionStatus.DISCONNECTED -> "Tap to establish tunnel"
                },
                color = Color.LightGray,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun TelemetryDashboardPill(
    label: String,
    value: String,
    subtext: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier
            .width(160.dp)
            .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(color.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = label, color = Color.Gray, fontSize = 10.sp)
            Text(text = value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(text = subtext, color = Color.LightGray, fontSize = 9.sp)
        }
    }
}

@Composable
fun PresetCardSelection(
    selected: AdaptivePreset,
    onSelected: (AdaptivePreset) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PresetOptionCard(
            preset = AdaptivePreset.STEALTH,
            title = "Stealth Profile",
            desc = "Automates aggressive frame fragmentation & ClientHello padding. Essential for hostile or highly censored network firewalls.",
            icon = Icons.Default.Security,
            selected = selected == AdaptivePreset.STEALTH,
            onClick = { onSelected(AdaptivePreset.STEALTH) }
        )
        PresetOptionCard(
            preset = AdaptivePreset.GAMING,
            title = "Gaming Low-Latency",
            desc = "Prioritizes high-speed UDP streams, maximizes socket capacity, and disables multiplex overhead for optimal lag reduction.",
            icon = Icons.Default.SportsEsports,
            selected = selected == AdaptivePreset.GAMING,
            onClick = { onSelected(AdaptivePreset.GAMING) }
        )
        PresetOptionCard(
            preset = AdaptivePreset.STANDARD,
            title = "Standard Mode",
            desc = "Optimally balanced parameters for everyday web-surfing. Features automated HTTP Mux buffers to minimize battery consumption.",
            icon = Icons.Default.Speed,
            selected = selected == AdaptivePreset.STANDARD,
            onClick = { onSelected(AdaptivePreset.STANDARD) }
        )
    }
}

@Composable
fun PresetOptionCard(
    preset: AdaptivePreset,
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFF0F172A))
            .border(
                width = 1.dp,
                color = if (selected) Color(0xFF10B981) else Color(0xFF334155),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    if (selected) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF1E293B),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (selected) Color(0xFF10B981) else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (selected) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF10B981),
                        modifier = Modifier.padding(2.dp)
                    ) {
                        Text(
                            text = "ACTIVE",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(text = desc, color = Color.LightGray, fontSize = 11.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
fun GatewaysTab(
    configs: List<ProxyConfig>,
    selectedConfig: ProxyConfig?,
    onSelectConfig: (String) -> Unit,
    isHealthChecking: Boolean,
    onTriggerHealthCheck: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Gateways Directory",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Automatically switches proxy route if current gateway fails health tests.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }

                    // Floating action to trigger health check
                    IconButton(
                        onClick = onTriggerHealthCheck,
                        modifier = Modifier.background(
                            if (isHealthChecking) Color(0xFFF59E0B) else Color(0xFF1E293B),
                            CircleShape
                        )
                    ) {
                        Icon(
                            imageVector = if (isHealthChecking) Icons.Default.Refresh else Icons.Default.FlashOn,
                            contentDescription = "Run Health Probe",
                            tint = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (configs.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddModerator,
                            contentDescription = "No proxies available",
                            tint = Color.Gray,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No proxy configurations parsed.",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Add a subscription url in Subscriptions tab to fetch nodes.",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                items(configs) { config ->
                    val isSelected = config.id == selectedConfig?.id
                    val isFailed = config.status == "FAILED"

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF1E293B).copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFF10B981) else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectConfig(config.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Protocol designator badge
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(
                                            if (isSelected) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFF0F172A),
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = config.protocol.take(3),
                                        color = if (isSelected) Color(0xFF10B981) else Color.LightGray,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = config.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${config.address}:${config.port}",
                                        color = Color.LightGray,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Health delay latency indicator
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isFailed) {
                                    Text(
                                        text = "TIMEOUT",
                                        color = Color(0xFFEF4444),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    val ping = config.latency
                                    val pingColor = when {
                                        ping < 80 -> Color(0xFF10B981)
                                        ping < 160 -> Color(0xFFF59E0B)
                                        else -> Color(0xFFEC4899)
                                    }
                                    Text(
                                        text = "$ping ms",
                                        color = pingColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            if (isFailed) Color(0xFFEF4444) else Color(0xFF10B981),
                                            CircleShape
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubscriptionsTab(
    subscriptions: List<Subscription>,
    onRemoveSub: (Int) -> Unit,
    onShowAddDialog: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Subscription Links",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Manage proxy sub URLs to pull configs automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }

                    IconButton(
                        onClick = onShowAddDialog,
                        modifier = Modifier.background(Color(0xFF10B981), CircleShape)
                    ) {
                        Icon(Icons.Default.AddLink, contentDescription = "Add Link", tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (subscriptions.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.BrokenImage,
                            contentDescription = "No subs",
                            tint = Color.Gray,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No subscription providers connected.",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Connect a link to automatically group and load bypass routes.",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                items(subscriptions) { sub ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CloudQueue,
                                        contentDescription = "Subscription provider",
                                        tint = Color(0xFF10B981)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = sub.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                IconButton(onClick = { onRemoveSub(sub.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete sub", tint = Color.LightGray)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = sub.url,
                                color = Color.Gray,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(sub.lastUpdated))
                            Text(
                                text = "Synced: $dateStr",
                                color = Color.LightGray,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConsoleTab(
    logs: List<SystemLog>,
    onClearLogs: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "System Audit Console",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Observe real-time handshake, fragmentation, and smart failovers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            IconButton(onClick = onClearLogs) {
                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear logs", tint = Color.Red)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { log ->
                    val dateStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                    val tagColor = when (log.tag.uppercase(Locale.getDefault())) {
                        "ENGINE", "SYSTEM" -> Color(0xFF10B981)
                        "STEALTH" -> Color(0xFFA855F7)
                        "GAMING" -> Color(0xFF3B82F6)
                        "FAILOVER" -> Color(0xFFF59E0B)
                        "ERROR" -> Color(0xFFEF4444)
                        else -> Color.Gray
                    }

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "[$dateStr] ",
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "[${log.tag}] ",
                            color = tagColor,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                        Text(
                            text = log.message,
                            color = Color.LightGray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddSubscriptionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("https://gardconfig.com/sub/premium_8231") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Add Sub Provider",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Provider Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Subscription Link") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.LightGray)
                    }
                    Button(
                        onClick = {
                            onConfirm(name, url)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Fetch Sub", color = Color.White)
                    }
                }
            }
        }
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
    val pre = "KMGTPE"[exp - 1]
    return String.format("%.1f %sB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtocolSelectorBottomSheet(
    onDismiss: () -> Unit,
    onSelectOption: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A), // Match cosmic slate theme
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Add Configuration Profile",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Group 1: Unified Import
            CategoryHeader(title = "UNIFIED IMPORT")
            BottomSheetItem(
                title = "Import from QRCode",
                desc = "Scan or parse QR configuration representations",
                icon = Icons.Default.QrCode,
                onClick = { onSelectOption("QRCODE") }
            )
            BottomSheetItem(
                title = "Import from Clipboard",
                desc = "Inspect and register connection URIs from system clipboard",
                icon = Icons.Default.ContentPaste,
                onClick = { onSelectOption("CLIPBOARD") }
            )
            BottomSheetItem(
                title = "Import Subscription Link",
                desc = "Connect an automated remote subscription URL (/sub/{token})",
                icon = Icons.Default.Link,
                onClick = { onSelectOption("SUBLINK") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Group 2: Group & Chain
            CategoryHeader(title = "GROUP & ROUTING CHAINS")
            BottomSheetItem(
                title = "Add [Policy group]",
                desc = "Multi-source node strategy selector (auto latency fallback)",
                icon = Icons.Default.GroupWork,
                onClick = { onSelectOption("POLICY_GROUP") }
            )
            BottomSheetItem(
                title = "Add [Proxy chain]",
                desc = "Chain proxy nodes in sequence (Double-Hop Routing)",
                icon = Icons.Default.Layers,
                onClick = { onSelectOption("PROXY_CHAIN") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Group 3: Legacy Protocols
            CategoryHeader(title = "LEGACY SECURE PROTOCOLS")
            BottomSheetItem(
                title = "Add [Shadowsocks]",
                desc = "High-performance encrypted byte-stream proxy",
                icon = Icons.Default.Lock,
                onClick = { onSelectOption("SHADOWSOCKS") }
            )
            BottomSheetItem(
                title = "Add [SOCKS]",
                desc = "Standard SOCKS5 secure authentication channel",
                icon = Icons.Default.Security,
                onClick = { onSelectOption("SOCKS") }
            )
            BottomSheetItem(
                title = "Add [HTTP]",
                desc = "Standard HTTP secure proxy tunnel",
                icon = Icons.Default.Http,
                onClick = { onSelectOption("HTTP") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Group 4: Next-Gen Protocols
            CategoryHeader(title = "NEXT-GENERATION PROTOCOLS")
            BottomSheetItem(
                title = "Add [VMess]",
                desc = "Traditional standard Xray core node profile",
                icon = Icons.Default.Cloud,
                onClick = { onSelectOption("VMESS") }
            )
            BottomSheetItem(
                title = "Add [VLESS]",
                desc = "Zero-overhead stateful pathway (Reality support)",
                icon = Icons.Default.CloudQueue,
                onClick = { onSelectOption("VLESS") }
            )
            BottomSheetItem(
                title = "Add [Trojan]",
                desc = "Stealthy routing with TLS masquerade",
                icon = Icons.Default.Shield,
                onClick = { onSelectOption("TROJAN") }
            )
            BottomSheetItem(
                title = "Add [Wireguard]",
                desc = "Fast, lightweight kernel-level VPN tunneling",
                icon = Icons.Default.SettingsEthernet,
                onClick = { onSelectOption("WIREGUARD") }
            )
            BottomSheetItem(
                title = "Add [Hysteria2]",
                desc = "Aggressive UDP-based congestion bypass protocol",
                icon = Icons.Default.FlashOn,
                onClick = { onSelectOption("HYSTERIA2") }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun CategoryHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFF10B981), // Emerald green group label
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(vertical = 6.dp)
    )
}

@Composable
fun BottomSheetItem(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(Color(0xFF1E293B), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = desc,
                color = Color.LightGray,
                fontSize = 11.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtocolManualBuilderDialog(
    protocolType: String,
    configsList: List<ProxyConfig>,
    onDismiss: () -> Unit,
    onConfirm: (ProxyConfig) -> Unit
) {
    // Shared states
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }

    // Protocol specific states
    // VLESS / VMess / Trojan
    var uuid by remember { mutableStateOf("") }
    var sni by remember { mutableStateOf("") }
    var realityPublicKey by remember { mutableStateOf("") }
    var realityShortId by remember { mutableStateOf("") }
    var realitySpiderX by remember { mutableStateOf("") }
    
    // VMess
    var alterId by remember { mutableStateOf("0") }
    var transportMethod by remember { mutableStateOf("WS") } // WS, TCP, gRPC
    
    // Trojan / SOCKS / HTTP
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    
    // Shadowsocks
    var ssCipher by remember { mutableStateOf("AES-256-GCM") } // AES-256-GCM, ChaCha20-Poly1305
    
    // Wireguard
    var wgPrivateKey by remember { mutableStateOf("") }
    var wgPublicKey by remember { mutableStateOf("") }
    var wgDns by remember { mutableStateOf("1.1.1.1") }
    var wgMtu by remember { mutableStateOf("1420") }
    
    // Hysteria2
    var hyObfsPassword by remember { mutableStateOf("") }
    var hyUpSpeed by remember { mutableStateOf("100") }
    var hyDownSpeed by remember { mutableStateOf("300") }
    
    // Policy Group
    var policyStrategy by remember { mutableStateOf("URLTest (Fastest Latency)") } // Selector, URLTest, Fallback
    val selectedNodesForPolicy = remember { mutableStateListOf<String>() }
    
    // Proxy Chain
    var chainFirstNodeId by remember { mutableStateOf("") }
    var chainSecondNodeId by remember { mutableStateOf("") }

    // QRCode / Clipboard Import representation
    var importText by remember { mutableStateOf("") }

    // Seed realistic defaults based on selected protocol type
    LaunchedEffect(protocolType) {
        val randSuffix = (100..999).random()
        when (protocolType) {
            "QRCODE" -> {
                name = "Import QRCode Link"
                importText = "vless://4303d8d6-7788-4444-9999-65239a9c@sg-01.gardconfig.com:443?security=reality&sni=yahoo.com&pbk=tHj_ZfSMyy_FMyvS&sid=e20b6016&spx=%2Fchrome#Gard%20Reality%20Node"
            }
            "CLIPBOARD" -> {
                name = "Clipboard Import"
                importText = "vmess://eyJhZGRyZXNzIjoianAtMDEuZ2FyZGNvbmZpZy5jb20iLCJwb3J0Ijo4NDQzLCJpZCI6ImI4M2NiYmRlLTIyMjItNDQ0NC04ODg4LTEyMzQ1Njc4OWFiYyIsImFpZCI6MCwicHN5IjoiaXAtMDEiLCJ0eXBlIjoibm9uZSIsIm5ldCI6IndzIiwicGF0aCI6Ii9nYXJkLWZsb3cifQ=="
            }
            "SUBLINK" -> {
                name = "Dynamic Multi-Source Sub"
                address = "https://gardconfig.com/sub/premium_fastroute"
            }
            "VLESS" -> {
                name = "VLESS Reality [US-$randSuffix]"
                address = "us-reality.gardconfig.com"
                port = "443"
                uuid = UUID.randomUUID().toString()
                sni = "yahoo.com"
                realityPublicKey = "tHj_ZfSMyy_FMyvS_E-ZfFMyyV4S_reality_pbk"
                realityShortId = "e20b6016"
                realitySpiderX = "/chrome"
            }
            "VMESS" -> {
                name = "VMess Premium [SG-$randSuffix]"
                address = "sg-vmess.gardconfig.com"
                port = "8443"
                uuid = UUID.randomUUID().toString()
                alterId = "0"
                transportMethod = "WS"
            }
            "TROJAN" -> {
                name = "Trojan Masquerade [DE-$randSuffix]"
                address = "de-trojan.gardconfig.com"
                port = "443"
                password = "gard_stealth_pass_$randSuffix"
                sni = "microsoft.com"
            }
            "SHADOWSOCKS" -> {
                name = "Shadowsocks Legacyshield [HK-$randSuffix]"
                address = "hk-ss.gardconfig.com"
                port = "8531"
                password = "ss_crypto_key_82319"
                ssCipher = "AES-256-GCM"
            }
            "SOCKS" -> {
                name = "SOCKS5 Proxy Tunnel [JP-$randSuffix]"
                address = "jp-socks.gardconfig.com"
                port = "1080"
                username = "gard_user"
                password = "socks5_password"
            }
            "HTTP" -> {
                name = "HTTP Secure Proxy [US-$randSuffix]"
                address = "us-http.gardconfig.com"
                port = "8080"
                username = "gard_http"
                password = "http_password"
            }
            "WIREGUARD" -> {
                name = "Wireguard Tunnel [DE-$randSuffix]"
                address = "de-wg.gardconfig.com"
                port = "51820"
                wgPrivateKey = "eK1Z_wireguard_private_key_demo_xxxx="
                wgPublicKey = "uY2Y_wireguard_public_key_demo_yyyy="
                wgDns = "1.1.1.1, 8.8.8.8"
                wgMtu = "1420"
            }
            "HYSTERIA2" -> {
                name = "Hysteria2 Blast [JP-$randSuffix]"
                address = "jp-hy2.gardconfig.com"
                port = "443"
                password = "hy2_fast_auth_secret"
                sni = "google.com"
                hyObfsPassword = "obfs_salts_xxx"
                hyUpSpeed = "150"
                hyDownSpeed = "500"
            }
            "POLICY_GROUP" -> {
                name = "Smart Routing Policy Group"
                policyStrategy = "URLTest (Fastest Latency)"
                // Auto-select first two configs
                configsList.take(2).forEach { selectedNodesForPolicy.add(it.id) }
            }
            "PROXY_CHAIN" -> {
                name = "Double-Hop Routing Chain"
                if (configsList.isNotEmpty()) {
                    chainFirstNodeId = configsList.first().id
                    if (configsList.size > 1) {
                        chainSecondNodeId = configsList[1].id
                    }
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .heightIn(max = 580.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (protocolType) {
                            "VLESS", "VMESS" -> Icons.Default.Cloud
                            "TROJAN" -> Icons.Default.Shield
                            "SHADOWSOCKS", "SOCKS", "HTTP" -> Icons.Default.Lock
                            "WIREGUARD" -> Icons.Default.SettingsEthernet
                            "HYSTERIA2" -> Icons.Default.FlashOn
                            "POLICY_GROUP" -> Icons.Default.GroupWork
                            "PROXY_CHAIN" -> Icons.Default.Layers
                            else -> Icons.Default.AddLink
                        },
                        contentDescription = "Builder Icon",
                        tint = Color(0xFF10B981)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = when (protocolType) {
                            "QRCODE" -> "Import from QR Code"
                            "CLIPBOARD" -> "Import from Clipboard"
                            "SUBLINK" -> "Import Remote Subscription"
                            "POLICY_GROUP" -> "Policy Group Custom Builder"
                            "PROXY_CHAIN" -> "Proxy Chain Builder"
                            else -> "$protocolType Protocol Builder"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.Gray.copy(alpha = 0.2f)))

                // Shared Form Fields
                if (protocolType != "QRCODE" && protocolType != "CLIPBOARD" && protocolType != "SUBLINK") {
                    CustomTextField(value = name, onValueChange = { name = it }, label = "Profile Name (Alias)")
                }

                if (protocolType != "QRCODE" && protocolType != "CLIPBOARD" && protocolType != "SUBLINK" && protocolType != "POLICY_GROUP" && protocolType != "PROXY_CHAIN") {
                    CustomTextField(value = address, onValueChange = { address = it }, label = "Server Host Address / IP")
                    CustomTextField(value = port, onValueChange = { port = it }, label = "Port")
                }

                // Protocol Specific Form Fields
                when (protocolType) {
                    "QRCODE", "CLIPBOARD" -> {
                        Text(
                            text = "Paste raw configuration payload or standard proxy URI representation (e.g. vless://, vmess://, trojan://):",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                        CustomTextField(value = importText, onValueChange = { importText = it }, label = "Raw Config Data / URI", singleLine = false, maxLines = 4)
                    }
                    "SUBLINK" -> {
                        CustomTextField(value = name, onValueChange = { name = it }, label = "Subscription Name (Alias)")
                        CustomTextField(value = address, onValueChange = { address = it }, label = "Subscription Remote Link URL (/sub/{token})")
                    }
                    "VLESS" -> {
                        CategorySubheader(title = "VLESS with Reality Configuration")
                        CustomTextField(value = uuid, onValueChange = { uuid = it }, label = "UUID Client Identifier")
                        CustomTextField(value = sni, onValueChange = { sni = it }, label = "SNI Server Name Indication")
                        CustomTextField(value = realityPublicKey, onValueChange = { realityPublicKey = it }, label = "Reality Public Key (pbk)")
                        CustomTextField(value = realityShortId, onValueChange = { realityShortId = it }, label = "Reality Short ID (sid)")
                        CustomTextField(value = realitySpiderX, onValueChange = { realitySpiderX = it }, label = "Reality SpiderX Path")
                    }
                    "VMESS" -> {
                        CategorySubheader(title = "VMess Profile Customization")
                        CustomTextField(value = uuid, onValueChange = { uuid = it }, label = "UUID Client Identifier")
                        CustomTextField(value = alterId, onValueChange = { alterId = it }, label = "AlterID (Default: 0)")
                        CustomTextField(value = transportMethod, onValueChange = { transportMethod = it }, label = "Transport Protocol (WS, TCP, gRPC)")
                    }
                    "TROJAN" -> {
                        CategorySubheader(title = "Trojan Authentication")
                        CustomTextField(value = password, onValueChange = { password = it }, label = "Trojan Password")
                        CustomTextField(value = sni, onValueChange = { sni = it }, label = "SNI Server Name Indication")
                    }
                    "SHADOWSOCKS" -> {
                        CategorySubheader(title = "Shadowsocks Symmetric Cipher")
                        CustomTextField(value = password, onValueChange = { password = it }, label = "Pre-Shared Key / Password")
                        CustomTextField(value = ssCipher, onValueChange = { ssCipher = it }, label = "Cipher Method (e.g. AES-256-GCM, ChaCha20-Poly1305)")
                    }
                    "SOCKS", "HTTP" -> {
                        CategorySubheader(title = "Access Authentication")
                        CustomTextField(value = username, onValueChange = { username = it }, label = "Auth Username (Optional)")
                        CustomTextField(value = password, onValueChange = { password = it }, label = "Auth Password (Optional)")
                    }
                    "WIREGUARD" -> {
                        CategorySubheader(title = "Wireguard Credentials")
                        CustomTextField(value = wgPrivateKey, onValueChange = { wgPrivateKey = it }, label = "Client Private Key")
                        CustomTextField(value = wgPublicKey, onValueChange = { wgPublicKey = it }, label = "Server Public Key")
                        CustomTextField(value = wgDns, onValueChange = { wgDns = it }, label = "DNS Servers")
                        CustomTextField(value = wgMtu, onValueChange = { wgMtu = it }, label = "MTU Size")
                    }
                    "HYSTERIA2" -> {
                        CategorySubheader(title = "Hysteria2 Dynamic Congestion Control")
                        CustomTextField(value = password, onValueChange = { password = it }, label = "Authentication Password")
                        CustomTextField(value = sni, onValueChange = { sni = it }, label = "SNI Server Name Indication")
                        CustomTextField(value = hyObfsPassword, onValueChange = { hyObfsPassword = it }, label = "Obfuscation Password (Optional)")
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                CustomTextField(value = hyUpSpeed, onValueChange = { hyUpSpeed = it }, label = "Max Upload (Mbps)")
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                CustomTextField(value = hyDownSpeed, onValueChange = { hyDownSpeed = it }, label = "Max Download (Mbps)")
                            }
                        }
                    }
                    "POLICY_GROUP" -> {
                        CategorySubheader(title = "Smart Failure & Load Balancing Strategy")
                        CustomTextField(value = policyStrategy, onValueChange = { policyStrategy = it }, label = "Strategy Mode (Selector, URLTest, Fallback)")
                        
                        Text(text = "Select member configurations:", fontSize = 11.sp, color = Color.LightGray)
                        configsList.forEach { node ->
                            val isChecked = selectedNodesForPolicy.contains(node.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChecked) selectedNodesForPolicy.remove(node.id)
                                        else selectedNodesForPolicy.add(node.id)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        if (isChecked) selectedNodesForPolicy.remove(node.id)
                                        else selectedNodesForPolicy.add(node.id)
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF10B981))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "${node.name} [${node.protocol}]", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                    "PROXY_CHAIN" -> {
                        CategorySubheader(title = "Double-Hop Pathway Chaining")
                        Text(text = "Define your custom multi-layered tunneling path. Traffic flows in order: Client -> Hop 1 Server -> Hop 2 Server -> Internet.", fontSize = 11.sp, color = Color.LightGray)
                        
                        Text(text = "First Tunnel Hop (Entry Gateway):", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        configsList.forEach { node ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { chainFirstNodeId = node.id }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = chainFirstNodeId == node.id,
                                    onClick = { chainFirstNodeId = node.id },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF10B981))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "${node.name} [${node.protocol}]", color = Color.White, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(text = "Second Tunnel Hop (Exit Gateway):", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        configsList.forEach { node ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { chainSecondNodeId = node.id }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = chainSecondNodeId == node.id,
                                    onClick = { chainSecondNodeId = node.id },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF10B981))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "${node.name} [${node.protocol}]", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.LightGray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (protocolType == "SUBLINK") {
                                onDismiss()
                            } else {
                                val finalProtocol = when (protocolType) {
                                    "QRCODE", "CLIPBOARD" -> {
                                        if (importText.contains("vless://")) "VLESS"
                                        else if (importText.contains("vmess://")) "VMESS"
                                        else if (importText.contains("ss://")) "SHADOWSOCKS"
                                        else "TROJAN"
                                    }
                                    "POLICY_GROUP" -> "GROUP"
                                    "PROXY_CHAIN" -> "CHAIN"
                                    else -> protocolType
                                }

                                val finalAddress = when (protocolType) {
                                    "QRCODE", "CLIPBOARD" -> {
                                        val extractedHost = importText.substringAfter("@", "").substringBefore(":", "imported-host.com")
                                        if (extractedHost.isEmpty()) "imported-host.com" else extractedHost
                                    }
                                    "POLICY_GROUP" -> "policy_group"
                                    "PROXY_CHAIN" -> "proxy_chain"
                                    else -> address
                                }

                                val finalPort = when (protocolType) {
                                    "QRCODE", "CLIPBOARD" -> {
                                        val portStr = importText.substringAfter("@", "").substringAfter(":", "").substringBefore("?", "443").substringBefore("/", "443")
                                        portStr.toIntOrNull() ?: 443
                                    }
                                    "POLICY_GROUP", "PROXY_CHAIN" -> 0
                                    else -> port.toIntOrNull() ?: 443
                                }

                                val finalName = when (protocolType) {
                                    "QRCODE" -> "Imported QR VLESS Reality"
                                    "CLIPBOARD" -> "Imported Clip VMess Profile"
                                    else -> name
                                }

                                val config = ProxyConfig(
                                    id = UUID.randomUUID().toString(),
                                    subId = 0, // Manual config
                                    name = finalName,
                                    protocol = finalProtocol,
                                    address = finalAddress,
                                    port = finalPort,
                                    latency = (40..150).random(), // Generate a realistic latency check
                                    status = "ACTIVE",
                                    isCurrentlySelected = false
                                )
                                onConfirm(config)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Save Profile", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun CategorySubheader(title: String) {
    Text(
        text = title,
        color = Color(0xFF10B981).copy(alpha = 0.8f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean = true,
    maxLines: Int = 1
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.LightGray) },
        colors = TextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = Color(0xFF0F172A),
            unfocusedContainerColor = Color(0xFF0F172A),
            focusedIndicatorColor = Color(0xFF10B981),
            unfocusedIndicatorColor = Color.Gray,
            cursorColor = Color(0xFF10B981)
        ),
        singleLine = singleLine,
        maxLines = maxLines,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    )
}
