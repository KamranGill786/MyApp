package com.example.viewmodel

import android.os.Build
import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DiagnosticLogEntity
import com.example.data.ToolkitRepository
import com.example.data.UsbEventEntity
import com.example.utils.ShellExecutor
import com.example.utils.SystemDiagnostics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Data structures for UI Binding
data class WifiAdapter(
    val name: String,
    val vendorId: String,
    val productId: String,
    val chipsetName: String,
    val driverName: String,
    val interfaceName: String,
    val isUsb: Boolean,
    val speed: String,
    val powerInfo: String,
    val isConnected: Boolean,
    val monitorModeSupported: Boolean,
    val managedModeSupported: Boolean,
    val bands: List<String>,
    val signalStrength: Int = -1
)

data class KernelModule(
    val name: String,
    val size: String,
    val usedBy: String,
    val isLoaded: Boolean,
    val isCompatible: Boolean,
    val dependencies: List<String>,
    val description: String,
    val path: String
)

data class SecurityTool(
    val name: String,
    val version: String,
    val isInstalled: Boolean,
    val compatibility: String,
    val description: String,
    val dependencies: String
)

data class SystemInfo(
    val androidVersion: String,
    val kernelVersion: String,
    val selinuxStatus: String,
    val isRooted: Boolean,
    val architecture: String
)

data class AppUiState(
    val selectedTab: Int = 0,
    val isDemoMode: Boolean = true,
    val isRooted: Boolean = false,
    val systemInfo: SystemInfo = SystemInfo("", "", "", false, ""),
    val connectedAdapters: List<WifiAdapter> = emptyList(),
    val kernelModules: List<KernelModule> = emptyList(),
    val securityTools: List<SecurityTool> = emptyList(),
    val terminalOutput: String = "WiFi Adapter Toolkit - Console Ready\n# Type a command or select presets below\n",
    val currentTerminalInput: String = "",
    val fileManagerPath: String = "/vendor/lib/modules",
    val fileManagerItems: List<String> = emptyList(),
    val dmesgLogs: String = "[   0.000000] Booting Linux on physical CPU 0x0000000000 [0x410fd083]\n[   0.000000] Linux version 6.1.25-v16\n",
    val exportReportData: String? = null
)

class MainViewModel(private val repository: ToolkitRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    // Expose database states reactively
    val diagnosticLogs: StateFlow<List<DiagnosticLogEntity>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val usbEvents: StateFlow<List<UsbEventEntity>> = repository.allUsbEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Initial detection
        refreshAllSystemInfo()
    }

    fun setTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun toggleDemoMode() {
        _uiState.update { it.copy(isDemoMode = !it.isDemoMode) }
        viewModelScope.launch {
            repository.insertLog(
                "SYSTEM", 
                "INFO", 
                "Switched Demo Mode to ${_uiState.value.isDemoMode}. Re-scanning adapters..."
            )
        }
        refreshAllSystemInfo()
    }

    fun setRootedMock(rooted: Boolean) {
        _uiState.update { 
            it.copy(
                isRooted = rooted,
                systemInfo = it.systemInfo.copy(isRooted = rooted)
            ) 
        }
        viewModelScope.launch {
            repository.insertLog("SYSTEM", "INFO", "Simulated root status updated to: $rooted")
        }
    }

    fun refreshAllSystemInfo() {
        val actualRoot = SystemDiagnostics.checkRootStatus()
        val currentRoot = if (_uiState.value.isDemoMode) _uiState.value.isRooted else actualRoot
        
        val sysInfo = SystemInfo(
            androidVersion = Build.VERSION.RELEASE,
            kernelVersion = SystemDiagnostics.getKernelVersion(),
            selinuxStatus = SystemDiagnostics.getSELinuxStatus(),
            isRooted = currentRoot,
            architecture = SystemDiagnostics.getArchitecture()
        )

        _uiState.update { it.copy(systemInfo = sysInfo, isRooted = currentRoot) }

        loadAdapters()
        loadKernelModules()
        loadSecurityTools()
        loadFileManager()
    }

    private fun loadAdapters() {
        if (_uiState.value.isDemoMode) {
            // Simulated High Density adapters
            val demoAdapter1 = WifiAdapter(
                name = "wlan1 (OTG USB)",
                vendorId = "0BDA",
                productId = "8812",
                chipsetName = "Realtek RTL8812AU",
                driverName = "8812au.ko (v5.6.4)",
                interfaceName = "wlan1",
                isUsb = true,
                speed = "USB 3.0 (5000 Mbps)",
                powerInfo = "5V @ 500mA Max",
                isConnected = true,
                monitorModeSupported = true,
                managedModeSupported = true,
                bands = listOf("2.4 GHz", "5.0 GHz")
            )
            val demoAdapter2 = WifiAdapter(
                name = "wlan0 (Internal)",
                vendorId = "Built-in",
                productId = "Built-in",
                chipsetName = "Broadcom BCM4389",
                driverName = "bcmdhd",
                interfaceName = "wlan0",
                isUsb = false,
                speed = "Internal PCIe",
                powerInfo = "Ultra Low Power",
                isConnected = true,
                monitorModeSupported = false,
                managedModeSupported = true,
                bands = listOf("2.4 GHz", "5.0 GHz", "6.0 GHz")
            )
            _uiState.update { it.copy(connectedAdapters = listOf(demoAdapter1, demoAdapter2)) }
        } else {
            // Read actual USB devices
            // We need a Context to check USB devices, we can check dynamically if possible,
            // or we fall back to general listing. In Compose we'll initialize it from Context.
            _uiState.update { it.copy(connectedAdapters = emptyList()) }
        }
    }

    fun scanRealUsbAdapters(context: Context) {
        if (_uiState.value.isDemoMode) return

        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val deviceList = usbManager.deviceList
        val list = mutableListOf<WifiAdapter>()

        // Always check internal wlan0
        list.add(
            WifiAdapter(
                name = "wlan0 (Internal)",
                vendorId = "Built-in",
                productId = "Built-in",
                chipsetName = "System Default Chipset",
                driverName = "Internal Driver",
                interfaceName = "wlan0",
                isUsb = false,
                speed = "PCIe/SDIO",
                powerInfo = "Internal Bus",
                isConnected = true,
                monitorModeSupported = false,
                managedModeSupported = true,
                bands = listOf("2.4 GHz", "5.0 GHz")
            )
        )

        for (device in deviceList.values) {
            val vid = device.vendorId
            val pid = device.productId
            
            // Lookup in chipset catalog
            val match = SystemDiagnostics.knownChipsets.find { 
                it.vendorId.equals(vid) || (it.vendorId == vid && it.productId == pid)
            }

            val adapter = if (match != null) {
                WifiAdapter(
                    name = "usb:${device.deviceName} (OTG)",
                    vendorId = String.format("%04X", vid),
                    productId = String.format("%04X", pid),
                    chipsetName = match.chipsetName,
                    driverName = match.driverName,
                    interfaceName = "wlan1 (usb)",
                    isUsb = true,
                    speed = "USB ${if (match.maxSpeedMbps > 150) "3.0" else "2.0"}",
                    powerInfo = "5V @ ${match.powerDrawMa}mA",
                    isConnected = true,
                    monitorModeSupported = match.monitorModeSupported,
                    managedModeSupported = true,
                    bands = if (match.maxSpeedMbps > 150) listOf("2.4 GHz", "5.0 GHz") else listOf("2.4 GHz")
                )
            } else {
                WifiAdapter(
                    name = "usb:${device.deviceName}",
                    vendorId = String.format("%04X", vid),
                    productId = String.format("%04X", pid),
                    chipsetName = "Generic USB Device (Class: ${device.deviceClass})",
                    driverName = "Unknown / Vendor-specific",
                    interfaceName = "usb_dev",
                    isUsb = true,
                    speed = "USB Generic",
                    powerInfo = "Unknown",
                    isConnected = true,
                    monitorModeSupported = false,
                    managedModeSupported = false,
                    bands = listOf("Unknown")
                )
            }
            list.add(adapter)
            
            // Log to database
            viewModelScope.launch {
                repository.logUsbEvent(
                    eventType = "ATTACH",
                    deviceName = device.deviceName,
                    vendorId = vid,
                    productId = pid,
                    isWifiAdapter = match != null,
                    hasPermission = usbManager.hasPermission(device),
                    powerStatus = "Attached (Active)"
                )
            }
        }
        _uiState.update { it.copy(connectedAdapters = list) }
    }

    private fun loadKernelModules() {
        if (_uiState.value.isDemoMode) {
            val list = listOf(
                KernelModule("8812au", "2457600", "0", true, true, listOf("cfg80211", "mac80211"), "Realtek 802.11ac NIC Driver", "/vendor/lib/modules/8812au.ko"),
                KernelModule("8821cu", "1854120", "0", false, true, listOf("cfg80211"), "Realtek 802.11ac dual band driver", "/vendor/lib/modules/8821cu.ko"),
                KernelModule("8188eu", "981240", "0", false, true, listOf("cfg80211"), "Realtek RTL8188EUS driver", "/vendor/lib/modules/8188eu.ko"),
                KernelModule("cfg80211", "983040", "1 8812au", true, true, emptyList(), "Wireless Configuration API", "/system/lib/modules/cfg80211.ko"),
                KernelModule("mac80211", "1228800", "1 8812au", true, true, listOf("cfg80211"), "IEEE 802.11 MAC Core", "/system/lib/modules/mac80211.ko"),
                KernelModule("rtw88_core", "752000", "0", false, false, listOf("cfg80211"), "Realtek modern driver core (Incompatible with kernel v6.1)", "/vendor/lib/modules/rtw88_core.ko")
            )
            _uiState.update { it.copy(kernelModules = list) }
        } else {
            val loaded = SystemDiagnostics.getLoadedKernelModulesReal()
            val list = loaded.map { name ->
                KernelModule(name, "Unknown", "-", true, true, emptyList(), "System Loaded Module", "/proc/modules")
            }
            _uiState.update { it.copy(kernelModules = list) }
        }
    }

    private fun loadSecurityTools() {
        val toolNames = listOf("aircrack-ng", "airodump-ng", "aireplay-ng", "iw")
        val tools = mutableListOf<SecurityTool>()

        for (name in toolNames) {
            if (_uiState.value.isDemoMode) {
                // Fully loaded tools for educational demonstration
                val tool = when (name) {
                    "aircrack-ng" -> SecurityTool(name, "v1.7", true, "Fully Compatible", "WEP/WPA packet cracker", "None")
                    "airodump-ng" -> SecurityTool(name, "v1.7", true, "Fully Compatible (Needs Monitor Mode)", "Packet capture & scanner", "8812au.ko driver")
                    "aireplay-ng" -> SecurityTool(name, "v1.7", true, "Fully Compatible (Needs Injection)", "Packet injection & deauth tool", "8812au.ko driver")
                    else -> SecurityTool(name, "v5.16", true, "Fully Compatible", "CLI Wireless device configuration utility", "Kernel cfg80211")
                }
                tools.add(tool)
            } else {
                val status = SystemDiagnostics.getSecurityToolStatusReal(name)
                val tool = SecurityTool(
                    name = name,
                    version = status.second,
                    isInstalled = status.first,
                    compatibility = if (status.first) "Detected on Path" else "Missing / Companion binary required",
                    description = when (name) {
                        "aircrack-ng" -> "WEP/WPA packet cracker"
                        "airodump-ng" -> "Packet capture & scanner"
                        "aireplay-ng" -> "Packet injection & deauth tool"
                        else -> "CLI Wireless device configuration utility"
                    },
                    dependencies = "Requires root + compatible Wi-Fi Driver"
                )
                tools.add(tool)
            }
        }
        _uiState.update { it.copy(securityTools = tools) }
    }

    private fun loadFileManager() {
        val currentPath = _uiState.value.fileManagerPath
        if (_uiState.value.isDemoMode) {
            val items = when (currentPath) {
                "/vendor/lib/modules" -> listOf("8812au.ko", "8821cu.ko", "8188eu.ko", "rtw88_core.ko", "readme.txt")
                "/lib/modules" -> listOf("cfg80211.ko", "mac80211.ko", "compat.ko")
                else -> listOf("session_diagnostic_report.txt", "tcpdump_local.pcap")
            }
            _uiState.update { it.copy(fileManagerItems = items) }
        } else {
            val directory = File(currentPath)
            if (directory.exists() && directory.isDirectory) {
                val list = directory.list()?.toList() ?: emptyList()
                _uiState.update { it.copy(fileManagerItems = list) }
            } else {
                _uiState.update { it.copy(fileManagerItems = listOf("Directory not accessible (Requires Root)")) }
            }
        }
    }

    fun changeDirectory(path: String) {
        _uiState.update { it.copy(fileManagerPath = path) }
        loadFileManager()
    }

    fun loadModule(module: KernelModule) {
        val isRooted = _uiState.value.isRooted
        val isDemo = _uiState.value.isDemoMode

        viewModelScope.launch {
            repository.insertLog("MODULE_MGR", "INFO", "Loading module ${module.name}...")
            
            if (isDemo) {
                // Simulate loading
                val updatedList = _uiState.value.kernelModules.map {
                    if (it.name == module.name) it.copy(isLoaded = true) else it
                }
                val newDmesg = _uiState.value.dmesgLogs + 
                    "[  ${System.currentTimeMillis() % 10000 / 100.0}] wifi_toolkit: insmod ${module.path} successfully loaded!\n" +
                    "[  ${System.currentTimeMillis() % 10000 / 100.0}] ${module.name}: module vermagic matched. Interface enabled.\n"

                _uiState.update { it.copy(kernelModules = updatedList, dmesgLogs = newDmesg) }
                repository.insertLog("MODULE_MGR", "INFO", "Successfully loaded module ${module.name} (Simulated)")
            } else {
                if (!isRooted) {
                    repository.insertLog("MODULE_MGR", "ERROR", "Root permission denied. Failed to load ${module.name}")
                    _uiState.update { 
                        it.copy(
                            dmesgLogs = it.dmesgLogs + "[ERR] insmod failed: Root privilege required!\n"
                        )
                    }
                } else {
                    val result = ShellExecutor.execute("insmod ${module.path}", true)
                    if (result.exitCode == 0) {
                        repository.insertLog("MODULE_MGR", "INFO", "Loaded module ${module.name}")
                        refreshAllSystemInfo()
                    } else {
                        repository.insertLog("MODULE_MGR", "ERROR", "insmod failed: ${result.error}")
                        _uiState.update { 
                            it.copy(
                                dmesgLogs = it.dmesgLogs + "[ERR] insmod ${module.name} failed: ${result.error}\n"
                            )
                        }
                    }
                }
            }
        }
    }

    fun unloadModule(module: KernelModule) {
        val isRooted = _uiState.value.isRooted
        val isDemo = _uiState.value.isDemoMode

        viewModelScope.launch {
            repository.insertLog("MODULE_MGR", "INFO", "Unloading module ${module.name}...")

            if (isDemo) {
                val updatedList = _uiState.value.kernelModules.map {
                    if (it.name == module.name) it.copy(isLoaded = false) else it
                }
                val newDmesg = _uiState.value.dmesgLogs + 
                    "[  ${System.currentTimeMillis() % 10000 / 100.0}] wifi_toolkit: rmmod ${module.name} completed.\n"

                _uiState.update { it.copy(kernelModules = updatedList, dmesgLogs = newDmesg) }
                repository.insertLog("MODULE_MGR", "INFO", "Successfully unloaded module ${module.name} (Simulated)")
            } else {
                if (!isRooted) {
                    repository.insertLog("MODULE_MGR", "ERROR", "Root permission denied. Failed to unload ${module.name}")
                } else {
                    val result = ShellExecutor.execute("rmmod ${module.name}", true)
                    if (result.exitCode == 0) {
                        repository.insertLog("MODULE_MGR", "INFO", "Unloaded module ${module.name}")
                        refreshAllSystemInfo()
                    } else {
                        repository.insertLog("MODULE_MGR", "ERROR", "rmmod failed: ${result.error}")
                    }
                }
            }
        }
    }

    fun updateTerminalInput(input: String) {
        _uiState.update { it.copy(currentTerminalInput = input) }
    }

    fun executeTerminalCommand(command: String) {
        if (command.isBlank()) return
        val isDemo = _uiState.value.isDemoMode
        val isRoot = _uiState.value.isRooted

        viewModelScope.launch {
            repository.insertLog("TERMINAL", "INFO", "Executed console command: $command")
            
            val currentOutput = _uiState.value.terminalOutput
            val appendHeader = "$currentOutput# $command\n"

            if (isDemo) {
                val simOutput = ShellExecutor.getSimulatedCommandOutput(command)
                _uiState.update { 
                    it.copy(
                        terminalOutput = "$appendHeader$simOutput\n\n",
                        currentTerminalInput = ""
                    )
                }
            } else {
                val result = ShellExecutor.execute(command, isRoot)
                val outputText = if (result.output.isNotBlank()) result.output else result.error
                val cleanOutput = outputText.ifBlank { "Command returned with exit code ${result.exitCode}" }
                _uiState.update { 
                    it.copy(
                        terminalOutput = "$appendHeader$cleanOutput\n\n",
                        currentTerminalInput = ""
                    )
                }
            }
        }
    }

    fun clearTerminal() {
        _uiState.update { it.copy(terminalOutput = "WiFi Adapter Toolkit - Console Cleared\n") }
    }

    fun triggerLocalPcapCapture() {
        // Safe simulated traffic capture offline analysis notification
        viewModelScope.launch {
            repository.insertLog("MONITOR_TRAFFIC", "INFO", "Began offline packet capture interface on wlan1mon (Simulated)")
            repository.insertLog("MONITOR_TRAFFIC", "INFO", "Offline traffic saved to local database directory: /data/user/0/com.aistudio.wifiadaptertoolkit.diag/files/local_traffic_capture.pcap")
            _uiState.update {
                it.copy(
                    terminalOutput = it.terminalOutput + "\n# capturing packets on wlan1mon locally (Offline analysis mode)...\n# Captured 254 packets [Beacon: 110, Data: 90, Probe: 54]\n# Capture log saved successfully.\n"
                )
            }
        }
    }

    fun generateDiagnosticReport() {
        viewModelScope.launch {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val sb = StringBuilder()
            sb.append("===================================================\n")
            sb.append("   WIFI ADAPTER TOOLKIT - DIAGNOSTIC SESSION REPORT\n")
            sb.append("===================================================\n")
            sb.append("Generated At: $timestamp\n")
            sb.append("Device Architecture: ${_uiState.value.systemInfo.architecture}\n")
            sb.append("Android Release: ${_uiState.value.systemInfo.androidVersion}\n")
            sb.append("Kernel Version: ${_uiState.value.systemInfo.kernelVersion}\n")
            sb.append("SELinux Mode: ${_uiState.value.systemInfo.selinuxStatus}\n")
            sb.append("Root Privileges Access: ${if (_uiState.value.isRooted) "GRANTED" else "NONE/DENIED"}\n")
            sb.append("Operational Mode: ${if (_uiState.value.isDemoMode) "DEMO RESEARCH SIMULATION" else "LIVE HARDWARE SCAN"}\n\n")

            sb.append("--- DETECTED WI-FI ADAPTERS ---\n")
            _uiState.value.connectedAdapters.forEachIndexed { i, adapter ->
                sb.append("[${i + 1}] Interface: ${adapter.interfaceName} | Name: ${adapter.name}\n")
                sb.append("    Chipset: ${adapter.chipsetName} | Driver: ${adapter.driverName}\n")
                sb.append("    Vendor ID: ${adapter.vendorId} | Product ID: ${adapter.productId}\n")
                sb.append("    USB speed: ${adapter.speed} | Bus draw: ${adapter.powerInfo}\n")
                sb.append("    Capabilities: Monitor mode: ${adapter.monitorModeSupported}, Packet injection: ${adapter.monitorModeSupported}\n")
                sb.append("    Bands supported: ${adapter.bands.joinToString(", ")}\n\n")
            }

            sb.append("--- DRIVER UTILITY & PEN-TEST COMPATIBILITY ---\n")
            _uiState.value.securityTools.forEach { tool ->
                sb.append("• Tool: ${tool.name} -> Status: ${if (tool.isInstalled) "INSTALLED (${tool.version})" else "MISSING"}\n")
                sb.append("  Kernel Compatibility: ${tool.compatibility}\n")
                sb.append("  Dependencies Status: ${tool.dependencies}\n")
            }
            sb.append("\n")

            sb.append("--- LOADED KERNEL MODULES ---\n")
            _uiState.value.kernelModules.filter { it.isLoaded }.forEach { mod ->
                sb.append("• ${mod.name}.ko (Size: ${mod.size} Bytes, Path: ${mod.path})\n")
            }
            sb.append("\n==================== END OF REPORT ====================\n")

            _uiState.update { it.copy(exportReportData = sb.toString()) }
            repository.insertLog("REPORT", "INFO", "Session diagnostic report compiled successfully.")
        }
    }

    fun dismissReport() {
        _uiState.update { it.copy(exportReportData = null) }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
            repository.insertLog("SYSTEM", "INFO", "Diagnostic log history cleared.")
        }
    }

    fun clearUsbEvents() {
        viewModelScope.launch {
            repository.clearUsbEvents()
            repository.insertLog("SYSTEM", "INFO", "USB connection logs history cleared.")
        }
    }
}

class MainViewModelFactory(private val repository: ToolkitRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
