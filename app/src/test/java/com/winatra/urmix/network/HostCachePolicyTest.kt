package com.winatra.urmix.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression guard for the FASE 5 §12.1 conservative policy: only URMIX-owned
 * hosts may be stored by the shared HTTP cache, everything else is `no-store`.
 */
class HostCachePolicyTest {

    @Test
    fun `whitelisted host is cacheable`() {
        assertEquals(
            HostCachePolicy.Policy.CACHEABLE,
            HostCachePolicy.policyFor("winatra.supabase.co")
        )
    }

    @Test
    fun `subdomains of the whitelisted host are cacheable`() {
        assertEquals(
            HostCachePolicy.Policy.CACHEABLE,
            HostCachePolicy.policyFor("api.winatra.supabase.co")
        )
    }

    @Test
    fun `lookalike hosts do not inherit the whitelist`() {
        assertEquals(
            HostCachePolicy.Policy.NO_STORE,
            HostCachePolicy.policyFor("notwinatra.supabase.co")
        )
        assertEquals(HostCachePolicy.Policy.NO_STORE, HostCachePolicy.policyFor("supabase.co"))
    }

    @Test
    fun `extractor hosts and their subdomains are never stored`() {
        assertEquals(HostCachePolicy.Policy.NO_STORE, HostCachePolicy.policyFor("youtube.com"))
        assertEquals(HostCachePolicy.Policy.NO_STORE, HostCachePolicy.policyFor("www.youtube.com"))
        assertEquals(
            HostCachePolicy.Policy.NO_STORE,
            HostCachePolicy.policyFor("r1---sn-abc123.googlevideo.com")
        )
        assertEquals(HostCachePolicy.Policy.NO_STORE, HostCachePolicy.policyFor("i.ytimg.com"))
    }

    @Test
    fun `unknown hosts default to no-store`() {
        assertEquals(HostCachePolicy.Policy.NO_STORE, HostCachePolicy.policyFor("example.com"))
    }

    @Test
    fun `null host is treated as no-store`() {
        assertEquals(HostCachePolicy.Policy.NO_STORE, HostCachePolicy.policyFor(null))
        assertFalse(HostCachePolicy.isExtractorHost(null))
    }

    @Test
    fun `extractor hosts are recognised for diagnostics`() {
        assertTrue(HostCachePolicy.isExtractorHost("youtube.com"))
        assertTrue(HostCachePolicy.isExtractorHost("cdn.sndcdn.com"))
        assertFalse(HostCachePolicy.isExtractorHost("winatra.supabase.co"))
    }
}
