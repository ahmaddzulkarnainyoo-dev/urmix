package com.winatra.urmix.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Regression guard for the FASE 5 §12.1 cache sizing: every cache must stay
 * bounded and low-RAM devices must get strictly smaller budgets.
 */
class CacheConfigTest {
    private val mib = 1024L * 1024L

    @Test
    fun `image disk cache uses the normal budget on regular devices`() {
        assertEquals(256L * mib, CacheConfig.imageDiskCacheBytes(false))
    }

    @Test
    fun `image disk cache is halved on low ram devices`() {
        assertEquals(128L * mib, CacheConfig.imageDiskCacheBytes(true))
    }

    @Test
    fun `http cache uses the normal budget on regular devices`() {
        assertEquals(64L * mib, CacheConfig.httpCacheBytes(false))
    }

    @Test
    fun `http cache is halved on low ram devices`() {
        assertEquals(32L * mib, CacheConfig.httpCacheBytes(true))
    }

    @Test
    fun `low ram devices get a smaller share of memory for images`() {
        assertEquals(0.25, CacheConfig.memoryCachePercent(false), 0.0)
        assertEquals(0.12, CacheConfig.memoryCachePercent(true), 0.0)
    }

    @Test
    fun `cache directory names stay stable`() {
        assertEquals("image_cache", CacheConfig.IMAGE_CACHE_DIR)
        assertEquals("http_cache", CacheConfig.HTTP_CACHE_DIR)
    }
}
