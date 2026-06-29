package com.example.utils

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

object ShellExecutor {

    data class CommandResult(
        val exitCode: Int,
        val output: String,
        val error: String
    )

    fun execute(command: String, isRoot: Boolean = false): CommandResult {
        return try {
            val process = if (isRoot) {
                val p = Runtime.getRuntime().exec("su")
                val os = OutputStreamWriter(p.outputStream)
                os.write(command + "\n")
                os.write("exit\n")
                os.flush()
                p
            } else {
                Runtime.getRuntime().exec(command)
            }

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))

            val output = StringBuilder()
            val error = StringBuilder()

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            while (errorReader.readLine().also { line = it } != null) {
                error.append(line).append("\n")
            }

            val exitCode = process.waitFor()
            CommandResult(exitCode, output.toString().trim(), error.toString().trim())
        } catch (e: Exception) {
            CommandResult(-1, "", e.message ?: "Unknown command error")
        }
    }

    // Pre-computed simulations for educational and demonstration purposes
    fun getSimulatedCommandOutput(command: String): String {
        val trimmed = command.trim().lowercase()
        return when {
            trimmed.contains("lsmod") -> """
                Module                  Size  Used by
                8812au               2457600  0 
                cfg80211              983040  1 8812au
                mac80211             1228800  1 8812au
                compat                 45056  2 cfg80211,mac80211
                usbcore               360448  2 8812au,usb_storage
            """.trimIndent()

            trimmed.contains("modinfo") && trimmed.contains("8812au") -> """
                filename:       /vendor/lib/modules/8812au.ko
                version:        v5.6.4.2_35491.20191025
                description:    Realtek Wireless Lan Driver
                license:        GPL
                author:         Realtek Semiconductor Corp.
                srcversion:     6D8EAEBCF45DAE01B8D176E
                alias:          usb:v0BDAp8812d*dc*dsc*dp*ic*isc*ip*in*
                depends:        cfg80211,mac80211,usbcore
                vermagic:       6.1.25-v16 SMP preempt mod_unload modversions aarch64
            """.trimIndent()

            trimmed.contains("uname") -> """
                Linux wifi-toolkit-host 6.1.25-v16-aarch64 #1 SMP PREEMPT Sun Jun 28 09:48:10 UTC 2026 aarch64 Android
            """.trimIndent()

            trimmed.contains("ip link") || trimmed.contains("ip link show") -> """
                1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN mode DEFAULT group default qlen 1000
                    link/loopback 00:00:00:00:00:00 brd 00:00:00:00:00:00
                2: wlan0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc mq state UP mode DORMANT group default qlen 3000
                    link/ether e0:d5:3d:fa:22:90 brd ff:ff:ff:ff:ff:ff
                3: wlan1: <BROADCAST,MULTICAST,UP> mtu 1500 qdisc mq state DOWN mode DEFAULT group default qlen 3000
                    link/ether 00:0e:8e:38:1e:72 brd ff:ff:ff:ff:ff:ff
            """.trimIndent()

            trimmed.contains("dmesg") -> """
                [   2.516082] usbcore: registered new interface driver usb-storage
                [  12.381502] usb 1-1: new high-speed USB device number 2 using dwc_otg
                [  12.492094] usb 1-1: New USB device found, idVendor=0bda, idProduct=8812, bcdDevice= 0.00
                [  12.492110] usb 1-1: New USB device strings: Mfr=1, Product=2, SerialNumber=3
                [  12.492121] usb 1-1: Product: 802.11ac NIC
                [  12.492131] usb 1-1: Manufacturer: Realtek
                [  12.510803] usbcore: registered new interface driver 8812au
                [  12.601944] 8812au: Loading firmware: rtl8812aufw.bin
                [  13.190412] 8812au: chipset RTL8812AU successfully initialized!
                [  13.201419] 8812au: wlan1 interface created in managed mode
            """.trimIndent()

            trimmed.contains("aircrack-ng") -> """
                Aircrack-ng 1.7  - (C) 2006-2022 Thomas d'Otreppe
                https://www.aircrack-ng.org

                  Usage: aircrack-ng [options] <.cap / .ivs file(s)>

                  Common Options:
                      -a <amode> : force attack mode (1/WEP, 2/WPA-PSK)
                      -e <essid> : target network ESSID selection
                      -b <bssid> : target network BSSID selection
                      -w <words> : path to wordlist dictionary
                      -q         : enable quiet mode (suppresses output)
            """.trimIndent()

            trimmed.contains("airodump-ng") -> """
                 CH  9 ][ Elapsed: 12 s ][ 2026-06-28 09:50 ][ wlan1mon active 

                 BSSID              PWR  Beacons    #Data, #/s  CH   MB   ENC CIPHER  AUTH ESSID
                 
                 00:11:22:33:44:55  -42       45       128    5   6  130   WPA2 CCMP   PSK  Secured_Home_5G
                 AA:BB:CC:DD:EE:FF  -68       32         4    0  11   54   WPA2 CCMP   PSK  Authorized_Demo_Network
                 11:22:33:44:55:66  -82       12         0    0   1  270   WPA3 CCMP   SAE  Lab_Test_Environment
                 
                 BSSID              STATION            PWR   Rate    Lost    Frames  Notes
                 00:11:22:33:44:55  AB:CD:EF:01:23:45  -38   54-54      0       120  Associated
            """.trimIndent()

            trimmed.contains("aireplay-ng") -> """
                Aireplay-ng 1.7  - (C) 2006-2022 Thomas d'Otreppe
                https://www.aircrack-ng.org

                  Usage: aireplay-ng <options> <replay interface>

                  Filter options:
                      -b bssid  : MAC address, Access Point
                      -d dmac   : MAC address, Destination
                      -s smac   : MAC address, Source
                      -h hmac   : MAC address, Source (alias)

                  Replay attacks (modes):
                      --deauth count : send deauthentications (authorized test only)
                      --fakeauth delay : fake authentication with AP
            """.trimIndent()

            trimmed.contains("iw") && trimmed.contains("wlan1") && trimmed.contains("info") -> """
                Interface wlan1
                    ifindex 3
                    wdev 0x1
                    addr 00:0e:8e:38:1e:72
                    type managed
                    wiphy 1
                    channel 36 (5180 MHz), width: 80 MHz, center1: 5210 MHz
                    txpower 20.00 dBm
            """.trimIndent()

            trimmed.contains("iw") && trimmed.contains("phy") -> """
                Wiphy phy1
                    max # scan SSIDs: 4
                    max scan IEs length: 2248 bytes
                    max # sched scan SSIDs: 0
                    Supported Ciphers:
                        * WEP40 (00-0f-ac:1)
                        * WEP104 (00-0f-ac:5)
                        * TKIP (00-0f-ac:2)
                        * CCMP-128 (00-0f-ac:4)
                    Frequencies:
                        * 2412 MHz [1] (20.0 dBm)
                        * 2417 MHz [2] (20.0 dBm)
                        * 2422 MHz [3] (20.0 dBm)
                        * 2427 MHz [4] (20.0 dBm)
                        * 2432 MHz [5] (20.0 dBm)
                        * 2437 MHz [6] (20.0 dBm)
                        * 2442 MHz [7] (20.0 dBm)
                        * 2447 MHz [8] (20.0 dBm)
                        * 2452 MHz [9] (20.0 dBm)
                        * 2457 MHz [10] (20.0 dBm)
                        * 2462 MHz [11] (20.0 dBm)
                        * 5180 MHz [36] (20.0 dBm)
                        * 5200 MHz [40] (20.0 dBm)
                        * 5220 MHz [44] (20.0 dBm)
                        * 5240 MHz [48] (20.0 dBm)
                    Supported interface modes:
                        * IBSS
                        * managed
                        * AP
                        * monitor
                        * mesh point
            """.trimIndent()

            trimmed.contains("iw") -> """
                iw [options] command
                Usage:	iw [options] dev <devname> subcommand
                    iw [options] phy <phyname> subcommand

                  Wireless diagnostic commands:
                      dev <devname> info
                      phy <phyname> info
                      dev <devname> scan
                      dev <devname> set type <mode>
            """.trimIndent()

            else -> "Command completed with exit code 0."
        }
    }
}
