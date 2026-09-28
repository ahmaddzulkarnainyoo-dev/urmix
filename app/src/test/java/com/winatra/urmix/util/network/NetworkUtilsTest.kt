package com.winatra.urmix.util.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression guard for the FASE 5 §12.2 connectivity rules. `isNetworkUsable` is
 * deliberately pure so the API-level difference (NET_CAPABILITY_VALIDATED only
 * exists from API 23 on) can be locked down without a device.
 */
class NetworkUtilsTest {

    @Test
    fun `validated internet capability is online`() {
        assertTrue(NetworkUtils.isNetworkUsable(true, true, true))
        assertTrue(NetworkUtils.isNetworkUsable(true, true, false))
    }

    @Test
    fun `unvalidated capability is still online below API 23`() {
        assertTrue(NetworkUtils.isNetworkUsable(true, false, false))
    }

    @Test
    fun `unvalidated capability is offline on API 23 and above`() {
        assertFalse(NetworkUtils.isNetworkUsable(true, false, true))
    }

    @Test
    fun `missing internet capability is always offline`() {
        assertFalse(NetworkUtils.isNetworkUsable(false, true, true))
        assertFalse(NetworkUtils.isNetworkUsable(false, false, false))
    }
}
