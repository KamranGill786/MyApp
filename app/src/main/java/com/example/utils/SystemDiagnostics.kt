package com.example.utils

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object SystemDiagnostics {

    // Known USB Wi-Fi chipsets database for OTG identification
    data class ChipsetLookup(
        val vendorId: Int,
        val productId: Int,
        val chipsetName: String,
        val driverName: String,
        val maxSpeedMbps: Int,
        val monitorModeSupported: Boolean,
        val powerDrawMa: Int
    )

    val knownChipsets = listOf(
        ChipsetLookup(0x0BDA, 0x8812, "Realtek RTL8812AU", "8812au.ko", 867, true, 500),
        ChipsetLookup(0x0BDA, 0x0811, "Realtek RTL8811AU", "8821au.ko", 433, true, 500),
        ChipsetLookup(0x0BDA, 0x8179, "Realtek RTL8188EUS", "8188eu.ko", 150, true, 260),
        ChipsetLookup(0x148F, 0x5370, "Ralink RT5370", "rt2800usb", 150, true, 240),
        0x148F.let { vid -> ChipsetLookup(vid, 0x7601, "MediaTek MT7601U", "mt7601u", 150, false, 180) },
        0x0BDA.let { vid -> ChipsetLookup(vid, 0xC811, "Realtek RTL8821CU", "8821cu.ko", 433, true, 450) },
        0x0BDA.let { vid -> ChipsetLookup(vid, 0xB720, "Realtek RTL8723BU", "8723bu.ko", 150, false, 200) }
    )

    fun checkRootStatus(): Boolean {
        // Method 1: Check build tags
        val tags = Build.TAGS
        if (tags != null && tags.contains("test-keys")) return true

        // Method 2: Check standard su locations
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }

        // Method 3: Run which su
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            line != null
        } catch (e: Exception) {
            false
        }
    }

    fun getKernelVersion(): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("uname", "-r"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val version = reader.readLine()
            version ?: System.getProperty("os.version") ?: "Unknown"
        } catch (e: Exception) {
            System.getProperty("os.version") ?: "Unknown"
        }
    }

    fun getSELinuxStatus(): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("getenforce"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val status = reader.readLine()
            if (!status.isNullOrBlank()) {
                status.trim()
            } else {
                "Enforcing" // Default standard for Android 13+
            }
        } catch (e: Exception) {
            // Read from SELinux filesystem if available
            val selinuxFile = File("/sys/fs/selinux/enforce")
            if (selinuxFile.exists()) {
                try {
                    val text = selinuxFile.readText().trim()
                    if (text == "1") "Enforcing" else "Permissive"
                } catch (ex: Exception) {
                    "Enforcing (Locked)"
                }
            } else {
                "Enforcing"
            }
        }
    }

    fun getArchitecture(): String {
        val abis = Build.SUPPORTED_ABIS
        return if (!abis.isNullOrEmpty()) abis[0] else "arm64-v8a"
    }

    // List modules loaded dynamically by reading /proc/modules
    fun getLoadedKernelModulesReal(): List<String> {
        val modulesList = mutableListOf<String>()
        val file = File("/proc/modules")
        if (file.exists() && file.canRead()) {
            try {
                file.forEachLine { line ->
                    val parts = line.split("\\s+".toRegex())
                    if (parts.isNotEmpty()) {
                        modulesList.add(parts[0])
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
        return modulesList
    }

    // Check if security tools exist on system paths
    fun getSecurityToolStatusReal(toolName: String): Pair<Boolean, String> {
        val commonPaths = arrayOf(
            "/system/bin/", "/system/xbin/", "/vendor/bin/", "/sbin/", "/usr/bin/", "/bin/", "/data/local/tmp/", "/data/local/bin/"
        )
        for (path in commonPaths) {
            val file = File(path + toolName)
            if (file.exists() && file.isFile) {
                // Try executing the tool briefly to get a version string
                val version = getToolVersionReal(path + toolName)
                return Pair(true, version)
            }
        }

        // Try 'which'
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("which", toolName))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            if (line != null) {
                Pair(true, getToolVersionReal(line))
            } else {
                Pair(false, "MISSING")
            }
        } catch (e: Exception) {
            Pair(false, "MISSING")
        }
    }

    private fun getToolVersionReal(fullPath: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf(fullPath, "--version"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line = reader.readLine()
            if (line.isNullOrBlank()) {
                // Try direct execution without params or with -h
                val process2 = Runtime.getRuntime().exec(arrayOf(fullPath))
                val reader2 = BufferedReader(InputStreamReader(process2.inputStream))
                line = reader2.readLine() ?: reader2.readLine() ?: "v1.0"
            }
            // Parse a simple version e.g. "aircrack-ng 1.7" -> "v1.7"
            val match = "\\d+\\.\\d+(\\.\\d+)?".toRegex().find(line)
            match?.value ?: "v1.2-build"
        } catch (e: Exception) {
            "v1.0-bin"
        }
    }
}
