package com.example.orbit.account

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkTargetsTest {

    @Test
    fun publicHttpsEndpointsDoNotNeedLocalNetworkPermission() {
        assertFalse(requiresLocalNetworkPermission("https://politics-std-loved-equipment.trycloudflare.com/"))
        assertFalse(requiresLocalNetworkPermission("https://api.orbit.example.com/"))
        assertFalse(requiresLocalNetworkPermission("https://8.8.8.8/"))
        assertFalse(requiresLocalNetworkPermission("https://172.32.0.1/"))
    }

    @Test
    fun privateAndLoopbackHostsNeedLocalNetworkPermission() {
        assertTrue(requiresLocalNetworkPermission("http://10.0.2.2:8080/"))
        assertTrue(requiresLocalNetworkPermission("http://10.24.8.3/"))
        assertTrue(requiresLocalNetworkPermission("http://127.0.0.1:8080/"))
        assertTrue(requiresLocalNetworkPermission("http://localhost:8080/"))
        assertTrue(requiresLocalNetworkPermission("http://192.168.1.20:8080/"))
        assertTrue(requiresLocalNetworkPermission("http://172.16.0.9/"))
        assertTrue(requiresLocalNetworkPermission("http://172.31.255.254/"))
        assertTrue(requiresLocalNetworkPermission("http://169.254.10.5/"))
        assertTrue(requiresLocalNetworkPermission("http://orbit-server:8080/"))
        assertTrue(requiresLocalNetworkPermission("http://nas.local/"))
        assertTrue(requiresLocalNetworkPermission("http://[::1]:8080/"))
        assertTrue(requiresLocalNetworkPermission("http://[fe80::1]/"))
        assertTrue(requiresLocalNetworkPermission("http://[fd12::1]/"))
    }

    @Test
    fun malformedUrlsFallBackToNotRequiringThePermission() {
        assertFalse(requiresLocalNetworkPermission(""))
        assertFalse(requiresLocalNetworkPermission("not a url"))
    }
}
