package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections

object NetworkUtils {

    data class NetworkStatus(
        val isConnected: Boolean,
        val isWifi: Boolean,
        val isHotspot: Boolean,
        val ipAddress: String?,
        val ssid: String?
    )

    fun getLocalIpv4Address(): String? {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())

            // Priority 1: Common Wi-Fi and Hotspot interfaces
            val preferredOrder = listOf("wlan0", "ap0", "wlan1", "eth0", "rndis0")
            for (name in preferredOrder) {
                val iface = interfaces.firstOrNull { it.name.equals(name, ignoreCase = true) && it.isUp && !it.isLoopback }
                if (iface != null) {
                    val address = findValidIpv4(iface)
                    if (address != null) return address
                }
            }

            // Priority 2: Any active non-loopback interface
            for (iface in interfaces) {
                if (iface.isUp && !iface.isLoopback) {
                    val address = findValidIpv4(iface)
                    if (address != null) return address
                }
            }
        } catch (_: Exception) {
        }
        return null
    }

    private fun findValidIpv4(networkInterface: NetworkInterface): String? {
        val addresses = Collections.list(networkInterface.inetAddresses)
        for (addr in addresses) {
            if (!addr.isLoopbackAddress && addr is Inet4Address) {
                val host = addr.hostAddress ?: continue
                if (!host.startsWith("127.") && !host.startsWith("169.254.")) {
                    return host
                }
            }
        }
        return null
    }

    fun getNetworkStatus(context: Context): NetworkStatus {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

        val activeNetwork = cm?.activeNetwork
        val caps = if (activeNetwork != null) cm.getNetworkCapabilities(activeNetwork) else null

        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isConnected = caps != null && (
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        )

        var ssid: String? = null
        if (isWifi && wm != null) {
            try {
                @Suppress("DEPRECATION")
                val wifiInfo = wm.connectionInfo
                val rawSsid = wifiInfo?.ssid
                if (!rawSsid.isNullOrBlank() && rawSsid != "<unknown ssid>") {
                    ssid = rawSsid.removePrefix("\"").removeSuffix("\"")
                }
            } catch (_: Exception) {
            }
        }

        val ipAddress = getLocalIpv4Address()
        // If IP is present but not Wi-Fi, it's often a Wi-Fi hotspot or tethering
        val isHotspot = !isWifi && ipAddress != null && (ipAddress.startsWith("192.168.43.") || ipAddress.startsWith("192.168.49.") || ipAddress.startsWith("172."))

        return NetworkStatus(
            isConnected = isConnected || isHotspot || ipAddress != null,
            isWifi = isWifi,
            isHotspot = isHotspot,
            ipAddress = ipAddress,
            ssid = ssid
        )
    }

    fun resolveHostName(address: InetAddress): String {
        return try {
            val host = address.canonicalHostName
            if (host.isNullOrBlank() || host == address.hostAddress) {
                address.hostAddress ?: "Client"
            } else {
                host
            }
        } catch (_: Exception) {
            address.hostAddress ?: "Client"
        }
    }
}
