package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DiagnosticLogEntity
import com.example.data.UsbEventEntity
import com.example.ui.theme.*
import com.example.viewmodel.KernelModule
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.WifiAdapter
import com.example.viewmodel.AppUiState
import com.example.viewmodel.SecurityTool
import com.example.viewmodel.SystemInfo
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolkitApp(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val logs by viewModel.diagnosticLogs.collectAsStateWithLifecycle()
    val usbEvents by viewModel.usbEvents.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Observe USB events dynamically on live mode
    LaunchedEffect(state.isDemoMode) {
        if (!state.isDemoMode) {
            viewModel.scanRealUsbAdapters(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SlateDarkBg,
                    titleContentColor = TextPrimary
                ),
                title = {
                    Column {
                        Text(
                            text = "SYSTEM DIAGNOSTIC",
                            style = MaterialTheme.typography.labelSmall,
                            color = LavLight,
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "WiFi Adapter Toolkit",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    // Demo mode toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "DEMO MODE",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.isDemoMode) LavLight else TextSecondary,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Switch(
                            checked = state.isDemoMode,
                            onCheckedChange = { viewModel.toggleDemoMode() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = LavLight,
                                checkedTrackColor = LavContainer
                            )
                        )
                    }

                    // Rooted Status badge
                    val badgeColor = if (state.isRooted) LavContainer else Color(0xFF422020)
                    val badgeTextColor = if (state.isRooted) LavOnContainer else Color(0xFFF87171)
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseAlpha"
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(badgeColor)
                            .border(1.dp, badgeTextColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .clickable { viewModel.setRootedMock(!state.isRooted) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (state.isRooted) GreenAccent else RedAccent)
                                .alpha(pulseAlpha)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.isRooted) "ROOTED" else "NON-ROOT",
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeTextColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SlateCardBg,
                tonalElevation = 8.dp
            ) {
                val tabs = listOf(
                    NavigationItem("Dashboard", Icons.Default.Dashboard),
                    NavigationItem("Drivers", Icons.Default.Memory),
                    NavigationItem("Terminal", Icons.Default.Terminal),
                    NavigationItem("USB/Files", Icons.Default.Usb),
                    NavigationItem("Reports", Icons.Default.Assessment)
                )

                tabs.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = state.selectedTab == index,
                        onClick = { viewModel.setTab(index) },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label, fontSize = 10.sp, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = LavDark,
                            selectedTextColor = LavLight,
                            indicatorColor = LavLight,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                }
            }
        },
        containerColor = SlateDarkBg
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Quick status row
            QuickStatusRow(state)

            // Content Area based on selected Tab
            Box(modifier = Modifier.weight(1f)) {
                when (state.selectedTab) {
                    0 -> DashboardTab(viewModel, state, logs)
                    1 -> DriversTab(viewModel, state)
                    2 -> TerminalTab(viewModel, state)
                    3 -> UsbFileManagerTab(viewModel, state, usbEvents)
                    4 -> ReportsLogsTab(viewModel, state, logs)
                }
            }
        }
    }

    // Export Dialog overlay
    state.exportReportData?.let { report ->
        Dialog(onDismissRequest = { viewModel.dismissReport() }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCardBg),
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Diagnostic Report Compiled",
                        style = MaterialTheme.typography.titleMedium,
                        color = LavLight,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Report content box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(SlateDarkBg, RoundedCornerShape(12.dp))
                            .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        LazyColumn {
                            item {
                                Text(
                                    text = report,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = { viewModel.dismissReport() },
                            colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                        ) {
                            Text("DISMISS")
                        }
                        
                        Row {
                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(report))
                                    Toast.makeText(context, "Report copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LavContainer, contentColor = LavOnContainer)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("COPY")
                            }
                        }
                    }
                }
            }
        }
    }
}

data class NavigationItem(val label: String, val icon: ImageVector)

@Composable
fun QuickStatusRow(state: AppUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatusCard(label = "Kernel", value = state.systemInfo.kernelVersion.split(" ").firstOrNull() ?: "6.1.25", modifier = Modifier.weight(1f))
        StatusCard(label = "SELinux", value = state.systemInfo.selinuxStatus, modifier = Modifier.weight(1f), isSuccess = state.systemInfo.selinuxStatus.equals("Enforcing", true))
        StatusCard(label = "Arch", value = state.systemInfo.architecture, modifier = Modifier.weight(1f))
    }
}

@Composable
fun StatusCard(label: String, value: String, modifier: Modifier = Modifier, isSuccess: Boolean? = null) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCardBg),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontSize = 9.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = when (isSuccess) {
                    true -> GreenAccent
                    false -> RedAccent
                    else -> TextPrimary
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ------------------------------------
// TAB 1: DASHBOARD
// ------------------------------------
@Composable
fun DashboardTab(viewModel: MainViewModel, state: AppUiState, logs: List<DiagnosticLogEntity>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Active Adapter Card
        val activeAdapter = state.connectedAdapters.find { it.isUsb && it.isConnected }
            ?: state.connectedAdapters.firstOrNull()

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = LavLight),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "ACTIVE ADAPTER",
                                style = MaterialTheme.typography.labelSmall,
                                color = LavDark.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = activeAdapter?.chipsetName ?: "No Adapter Detected",
                                style = MaterialTheme.typography.titleLarge,
                                color = LavDark,
                                fontWeight = FontWeight.Bold
                            )
                            if (activeAdapter != null) {
                                Text(
                                    text = "VID: ${activeAdapter.vendorId} | PID: ${activeAdapter.productId}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = LavDark.copy(alpha = 0.8f)
                                )
                            }
                        }
                        
                        // Icon Circle
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(LavDark)
                                .padding(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Adapter Info",
                                tint = LavLight,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    if (activeAdapter != null) {
                        Divider(color = LavDark.copy(alpha = 0.15f))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "CAPABILITIES",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LavDark.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (activeAdapter.monitorModeSupported) {
                                        CapabilityBadge("MONITOR")
                                        CapabilityBadge("INJECTION")
                                    } else {
                                        CapabilityBadge("MANAGED ONLY")
                                    }
                                    if (activeAdapter.bands.contains("5.0 GHz")) {
                                        CapabilityBadge("5GHz")
                                    }
                                }
                            }
                            
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "STATUS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LavDark.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Up & Running",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = LavDark
                                )
                            }
                        }
                    } else {
                        // Empty Warning
                        Text(
                            text = "Please attach a compatible external USB OTG Wi-Fi Adapter or scan internal system parameters.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LavDark.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Mid Row: Security Tools & Module Stats
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left Card: Security Tools Status
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCardBg),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1.1f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "SECURITY TOOLS",
                            style = MaterialTheme.typography.labelSmall,
                            color = LavLight,
                            fontWeight = FontWeight.Bold
                        )
                        
                        state.securityTools.forEach { tool ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tool.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (tool.isInstalled) tool.version else "MISS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (tool.isInstalled) GreenAccent else RedAccent,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                // Right Card: Kernel Module count & info
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCardBg),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(0.9f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "KERNEL MODULES",
                            style = MaterialTheme.typography.labelSmall,
                            color = LavLight,
                            fontWeight = FontWeight.Bold
                        )

                        val loadedCount = state.kernelModules.count { it.isLoaded }
                        Column {
                            Text(
                                text = "$loadedCount",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Light,
                                color = TextPrimary
                            )
                            Text(
                                text = "Modules Loaded",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(LavContainer.copy(alpha = 0.3f))
                                .padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Status ok",
                                tint = LavLight,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "8812au.ko verified",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = LavLight
                            )
                        }
                    }
                }
            }
        }

        // Bottom Section: Mini Console Output Terminal Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDarkBg),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Terminal Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SlateBorder)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Terminal Preview: /dev/pts/0",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(RedAccent))
                            Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(LavLight))
                        }
                    }

                    // Terminal Preview lines
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SlateDarkBg)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TerminalLine(command = "lsmod | grep 8812", output = "8812au    2457600  0 - Live 0x000000\ncfg80211   983040  1 8812au")
                        TerminalLine(command = "dmesg | tail -n 1", output = "[82.1] usb 1-1: RTL8812AU fw ready and wlan1 interface created")
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "# ", color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            val infiniteTransition = rememberInfiniteTransition(label = "cursor")
                            val cursorAlpha by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(500),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "cursorAlpha"
                            )
                            Box(
                                modifier = Modifier
                                    .width(6.dp)
                                    .height(11.dp)
                                    .background(LavLight)
                                    .alpha(cursorAlpha)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CapabilityBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(alpha = 0.2f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = LavDark
        )
    }
}

@Composable
fun TerminalLine(command: String, output: String) {
    Column {
        Row {
            Text(text = "# ", color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            Text(text = command, color = LavLight, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        }
        Text(
            text = output,
            color = TextSecondary,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

// ------------------------------------
// TAB 2: DRIVERS & MODULES MANAGER
// ------------------------------------
@Composable
fun DriversTab(viewModel: MainViewModel, state: AppUiState) {
    var searchQueries by remember { mutableStateOf("") }
    val filteredModules = state.kernelModules.filter {
        it.name.contains(searchQueries, ignoreCase = true) || 
        it.description.contains(searchQueries, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Warning if missing firmware detected
        val showWarning = state.kernelModules.any { it.name == "rtw88_core" }
        if (showWarning) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF422020)),
                border = BorderStroke(1.dp, RedAccent.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = "Alert", tint = RedAccent)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "FIRMWARE STATUS ALERT",
                            style = MaterialTheme.typography.labelSmall,
                            color = RedAccent,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Missing firmware file 'rtw88/rtw8822c_fw.bin'. Diagnostic verify fails for rtw88_core.ko. Please export module details.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQueries,
            onValueChange = { searchQueries = it },
            placeholder = { Text("Search modules, drivers...", color = TextSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = LavLight,
                unfocusedBorderColor = SlateBorder,
                focusedContainerColor = SlateCardBg,
                unfocusedContainerColor = SlateCardBg
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        // Title and controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Kernel Module Repositories (.ko)",
                style = MaterialTheme.typography.titleSmall,
                color = LavLight,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = { viewModel.refreshAllSystemInfo() },
                colors = ButtonDefaults.buttonColors(containerColor = SlateCardBg, contentColor = TextPrimary),
                border = BorderStroke(1.dp, SlateBorder),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Rescan", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("RESCAN", fontSize = 10.sp)
            }
        }

        // List of Modules
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredModules) { mod: KernelModule ->
                ModuleItemCard(mod, onLoad = { viewModel.loadModule(mod) }, onUnload = { viewModel.unloadModule(mod) })
            }
        }

        // Live Kernel dmesg logs pane
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateCardBg),
            border = BorderStroke(1.dp, SlateBorder),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "DMESG KERNEL LOADING LOGS",
                    style = MaterialTheme.typography.labelSmall,
                    color = LavLight,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(SlateDarkBg, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    LazyColumn {
                        item {
                            Text(
                                text = state.dmesgLogs,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModuleItemCard(module: KernelModule, onLoad: () -> Unit, onUnload: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCardBg),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = module.name + ".ko",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (module.isLoaded) GreenAccent.copy(alpha = 0.15f) else TextSecondary.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (module.isLoaded) "LOADED" else "UNLOADED",
                                color = if (module.isLoaded) GreenAccent else TextSecondary,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Size: ${module.size} B | Used: ${module.usedBy}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )
                }

                // Load/Unload Action Button
                Button(
                    onClick = { if (module.isLoaded) onUnload() else onLoad() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (module.isLoaded) Color(0xFF422020) else LavContainer,
                        contentColor = if (module.isLoaded) RedAccent else LavOnContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (module.isLoaded) "UNLOAD" else "LOAD", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = module.description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            if (module.dependencies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Dependencies: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        module.dependencies.forEach { dep ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SlateDarkBg)
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(dep, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = LavLight)
                            }
                        }
                    }
                }
            }

            if (!module.isCompatible) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "⚠ WARNING: High Risk vermagic mismatch with Linux Kernel core.",
                    color = RedAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ------------------------------------
// TAB 3: CONSOLE / TERMINAL
// ------------------------------------
@Composable
fun TerminalTab(viewModel: MainViewModel, state: AppUiState) {
    var commandInput by remember { mutableStateOf("") }
    val presets = listOf("lsmod", "modinfo 8812au", "uname -a", "ip link", "dmesg | tail", "airodump-ng wlan1mon")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Quick instruction banner
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateCardBg),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = "Info", tint = LavLight)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Tap on preset command tags below to paste and run instantly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )
            }
        }

        // Preset command tags
        Text(
            text = "SAFE PRESETS",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            fontWeight = FontWeight.Bold
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presets.take(3).forEach { p ->
                PresetChip(p, onClick = { viewModel.executeTerminalCommand(p) })
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presets.drop(3).forEach { p ->
                PresetChip(p, onClick = { viewModel.executeTerminalCommand(p) })
            }
        }

        // Main Console Terminal Pane
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateDarkBg),
            border = BorderStroke(1.dp, SlateBorder),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Terminal Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SlateBorder)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Code, contentDescription = "Console", tint = TextPrimary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Interactive Shell: su / bin / sh",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )
                    }
                    IconButton(
                        onClick = { viewModel.clearTerminal() },
                        modifier = Modifier.size(16.dp)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }

                // Dynamic shell text area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    LazyColumn(
                        reverseLayout = true,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Text(
                                text = state.terminalOutput,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = LavLight
                            )
                        }
                    }
                }
            }
        }

        // Pen-testing helper buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.triggerLocalPcapCapture() },
                colors = ButtonDefaults.buttonColors(containerColor = LavContainer, contentColor = LavOnContainer),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.WifiTethering, contentDescription = "Sniff", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("CAPTURE TRAFFIC", fontSize = 11.sp)
            }
        }

        // Custom command Input field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = commandInput,
                onValueChange = { commandInput = it },
                placeholder = { Text("Enter shell command...", color = TextSecondary, fontSize = 12.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = LavLight,
                    unfocusedBorderColor = SlateBorder,
                    focusedContainerColor = SlateCardBg,
                    unfocusedContainerColor = SlateCardBg
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (commandInput.isNotBlank()) {
                        viewModel.executeTerminalCommand(commandInput)
                        commandInput = ""
                    }
                }),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            )

            FloatingActionButton(
                onClick = {
                    if (commandInput.isNotBlank()) {
                        viewModel.executeTerminalCommand(commandInput)
                        commandInput = ""
                    }
                },
                containerColor = LavLight,
                contentColor = LavDark,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(50.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Run")
            }
        }
    }
}

@Composable
fun PresetChip(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SlateCardBg)
            .border(1.dp, SlateBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = LavLight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ------------------------------------
// TAB 4: USB & FILE MANAGER
// ------------------------------------
@Composable
fun UsbFileManagerTab(viewModel: MainViewModel, state: AppUiState, usbEvents: List<UsbEventEntity>) {
    var selectedSubTab by remember { mutableStateOf(0) } // 0: USB Events, 1: File Browser

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Switcher header
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = SlateDarkBg,
            contentColor = LavLight,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedSubTab]),
                    color = LavLight
                )
            }
        ) {
            Tab(selected = selectedSubTab == 0, onClick = { selectedSubTab = 0 }) {
                Text("USB ATTACH LOGS", modifier = Modifier.padding(12.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Tab(selected = selectedSubTab == 1, onClick = { selectedSubTab = 1 }) {
                Text("KO FILE BROWSER", modifier = Modifier.padding(12.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (selectedSubTab == 0) {
            // Tab 0: USB Device events
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live OTG Event Monitor",
                    style = MaterialTheme.typography.titleSmall,
                    color = LavLight,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = { viewModel.clearUsbEvents() },
                    colors = ButtonDefaults.textButtonColors(contentColor = RedAccent)
                ) {
                    Text("CLEAR ALL")
                }
            }

            if (usbEvents.isEmpty() && state.isDemoMode) {
                // Pop some simulated usb logs for richness
                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val mockEvents = listOf(
                    UsbEventEntity(id = 1, timestamp = System.currentTimeMillis() - 50000, eventType = "ATTACH", deviceName = "Realtek 802.11ac Adapter", vendorId = 0x0BDA, productId = 0x8812, isWifiAdapter = true, hasPermission = true, powerStatus = "Self-Powered (500mA)"),
                    UsbEventEntity(id = 2, timestamp = System.currentTimeMillis() - 150000, eventType = "DETACH", deviceName = "Generic Mouse", vendorId = 0x046D, productId = 0xC077, isWifiAdapter = false, hasPermission = false, powerStatus = "Suspended")
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    items(mockEvents) { ev ->
                        UsbEventCard(ev)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(usbEvents) { event ->
                        UsbEventCard(event)
                    }
                }
            }
        } else {
            // Tab 1: Module file manager
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Directory: ${state.fileManagerPath}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = LavLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Quick navigation tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.changeDirectory("/vendor/lib/modules") },
                    colors = ButtonDefaults.buttonColors(containerColor = if (state.fileManagerPath == "/vendor/lib/modules") LavContainer else SlateCardBg, contentColor = TextPrimary),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("/vendor/lib", fontSize = 10.sp)
                }
                Button(
                    onClick = { viewModel.changeDirectory("/lib/modules") },
                    colors = ButtonDefaults.buttonColors(containerColor = if (state.fileManagerPath == "/lib/modules") LavContainer else SlateCardBg, contentColor = TextPrimary),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("/lib", fontSize = 10.sp)
                }
                Button(
                    onClick = { viewModel.changeDirectory("/sdcard/Download") },
                    colors = ButtonDefaults.buttonColors(containerColor = if (state.fileManagerPath == "/sdcard/Download") LavContainer else SlateCardBg, contentColor = TextPrimary),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("/Download", fontSize = 10.sp)
                }
            }

            // File items
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(SlateCardBg, RoundedCornerShape(20.dp))
                    .border(1.dp, SlateBorder, RoundedCornerShape(20.dp))
                    .padding(12.dp)
            ) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.fileManagerItems) { filename ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SlateDarkBg)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val icon = if (filename.endsWith(".ko")) Icons.Default.Extension else Icons.Default.InsertDriveFile
                                Icon(icon, contentDescription = "File type", tint = LavLight, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = filename,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimary
                                )
                            }
                            
                            if (filename.endsWith(".ko")) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "IMPORT",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LavLight,
                                        modifier = Modifier
                                            .clickable { }
                                            .padding(4.dp)
                                    )
                                    Text(
                                        text = "EXPORT",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary,
                                        modifier = Modifier
                                            .clickable { }
                                            .padding(4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UsbEventCard(event: UsbEventEntity) {
    val date = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(event.timestamp))
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCardBg),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isAttach = event.eventType.equals("ATTACH", true)
            Icon(
                imageVector = if (isAttach) Icons.Default.Usb else Icons.Default.UsbOff,
                contentDescription = "USB",
                tint = if (isAttach) GreenAccent else RedAccent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.deviceName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "VID: ${String.format("%04X", event.vendorId)} | PID: ${String.format("%04X", event.productId)} • Power: ${event.powerStatus}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary
                )
            }
            Text(
                text = date,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )
        }
    }
}

// ------------------------------------
// TAB 5: REPORTS & raw LOGS PERSISTENCE
// ------------------------------------
@Composable
fun ReportsLogsTab(viewModel: MainViewModel, state: AppUiState, logs: List<DiagnosticLogEntity>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Report generator block
        Card(
            colors = CardDefaults.cardColors(containerColor = LavContainer),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "COMPREHENSIVE PEN-TEST AUDIT REPORT",
                    style = MaterialTheme.typography.labelSmall,
                    color = LavOnContainer.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Compile Adapter Diagnosis & Session Logs",
                    style = MaterialTheme.typography.titleMedium,
                    color = LavOnContainer,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Compiles USB hardware attach streams, dmesg loading results, security tool states, loaded kernel module maps, and exports a text format audit manifest.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LavOnContainer.copy(alpha = 0.8f)
                )
                
                Button(
                    onClick = { viewModel.generateDiagnosticReport() },
                    colors = ButtonDefaults.buttonColors(containerColor = LavLight, contentColor = LavDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Assessment, contentDescription = "Export")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("COMPILE & EXPORT SESSION REPORT", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Logs history header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Toolkit Logs DB History",
                style = MaterialTheme.typography.titleSmall,
                color = LavLight,
                fontWeight = FontWeight.Bold
            )
            TextButton(
                onClick = { viewModel.clearLogs() },
                colors = ButtonDefaults.textButtonColors(contentColor = RedAccent)
            ) {
                Text("CLEAR HISTORY")
            }
        }

        // Room raw diagnostic logs list
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(SlateCardBg, RoundedCornerShape(20.dp))
                .border(1.dp, SlateBorder, RoundedCornerShape(20.dp))
                .padding(12.dp)
        ) {
            if (logs.isEmpty()) {
                // Show default list for richness if DB is pristine
                val dummyLogs = listOf(
                    DiagnosticLogEntity(id = 1, tag = "SYSTEM", level = "INFO", message = "WiFi Adapter Toolkit initialized successfully."),
                    DiagnosticLogEntity(id = 2, tag = "USB_MONITOR", level = "INFO", message = "Bound USB connection BroadcastReceiver filters."),
                    DiagnosticLogEntity(id = 3, tag = "DRIVERS", level = "WARN", message = "Driver file rtw88_core.ko is not fully compatible with Linux 6.1.")
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(dummyLogs) { log ->
                        LogLineItem(log)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(logs) { log ->
                        LogLineItem(log)
                    }
                }
            }
        }
    }
}

@Composable
fun LogLineItem(log: DiagnosticLogEntity) {
    val date = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
    val levelColor = when (log.level) {
        "ERROR" -> RedAccent
        "WARN" -> Color(0xFFFBBF24)
        else -> GreenAccent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SlateDarkBg)
            .padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "[$date]",
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            color = TextSecondary,
            modifier = Modifier.padding(end = 6.dp)
        )
        Text(
            text = log.level,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = levelColor,
            modifier = Modifier.padding(end = 6.dp)
        )
        Text(
            text = "[${log.tag}]",
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            color = LavLight,
            modifier = Modifier.padding(end = 6.dp)
        )
        Text(
            text = log.message,
            fontSize = 10.sp,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}
