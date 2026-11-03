package com.example.orbit.account

import java.net.URI

/**
 * Android 17 only gates sockets to the local network behind
 * [android.Manifest.permission.ACCESS_LOCAL_NETWORK]; public HTTPS endpoints
 * must keep working when that runtime permission is not granted.
 *
 * The classification is deliberately small and testable: loopback, private
 * IPv4 ranges, IPv6 link-local / ULA, mDNS / dotless hostnames and `.local`
 * names need the permission; public DNS names and public IPs do not.
 */
fun requiresLocalNetworkPermission(baseUrl: String): Boolean {
    val host = runCatching { URI(baseUrl).host }.getOrNull()
        ?.trim()
        ?.lowercase()
        ?.removeSurrounding("[", "]")
        ?.trimEnd('.')
        ?.takeIf { it.isNotEmpty() }
        ?: return false

    if (host == "localhost" || host.endsWith(".localhost") || host.endsWith(".local")) {
        return true
    }

    if (host.contains(':')) {
        // IPv6 loopback, link-local and unique-local addresses.
        return host == "::1" ||
            host == "0:0:0:0:0:0:0:1" ||
            host.startsWith("fe80:") ||
            host.startsWith("fc") ||
            host.startsWith("fd")
    }

    val octets = host.split('.').mapNotNull { it.toIntOrNull() }
    if (octets.size == 4 && octets.all { it in 0..255 }) {
        return when {
            octets[0] == 0 || octets[0] == 10 || octets[0] == 127 -> true
            octets[0] == 169 && octets[1] == 254 -> true
            octets[0] == 172 && octets[1] in 16..31 -> true
            octets[0] == 192 && octets[1] == 168 -> true
            else -> false
        }
    }

    // Single-label hostnames such as "orbit-server" resolve through mDNS or the
    // local DNS search domain, so treat them as local too.
    return !host.contains('.')
}
