package com.winatra.urmix.network

import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * Regression guard for the FASE 5 §12.1 interceptors: the whitelist decides
 * what is stored, everything else is pinned to `no-store`, and the offline
 * interceptor only rewrites whitelisted hosts while offline.
 *
 * Chain stubbing/verification lives in [HttpChainStubber] (Java) because OkHttp 5's
 * Kotlin `proceed(request)` rejects the null returned by Mockito's matchers.
 */
class HttpCacheInterceptorsTest {
    private val plainText = "text/plain".toMediaType()

    @Test
    fun `cacheable response is stamped with the cacheable cache control`() {
        val chain = fakeChain("https://winatra.supabase.co/rest/v1/config")
        val response = HttpCacheControlInterceptor().intercept(chain)

        assertEquals(
            HostCachePolicy.CACHEABLE_CACHE_CONTROL,
            response.header("Cache-Control")
        )
    }

    @Test
    fun `extractor request and response are pinned to no-store`() {
        val chain = fakeChain("https://www.youtube.com/watch?v=abc123")
        val response = HttpCacheControlInterceptor().intercept(chain)

        val forwarded = chainForwardedRequest(chain)
        assertEquals(
            HostCachePolicy.NO_STORE_CACHE_CONTROL,
            forwarded.header("Cache-Control")
        )
        assertEquals(
            HostCachePolicy.NO_STORE_CACHE_CONTROL,
            response.header("Cache-Control")
        )
    }

    @Test
    fun `offline interceptor serves whitelisted hosts from cache`() {
        val chain = fakeChain("https://winatra.supabase.co/rest/v1/config")
        OfflineCacheInterceptor(isOnline = { false }).intercept(chain)

        val forwarded = chainForwardedRequest(chain)
        assertEquals(CacheControl.FORCE_CACHE.toString(), forwarded.cacheControl.toString())
    }

    @Test
    fun `offline interceptor leaves extractor hosts untouched`() {
        val chain = fakeChain("https://www.youtube.com/watch?v=abc123")
        val original = chain.request()
        OfflineCacheInterceptor(isOnline = { false }).intercept(chain)

        val forwarded = chainForwardedRequest(chain)
        assertSame(original, forwarded)
    }

    @Test
    fun `online interceptor leaves whitelisted hosts untouched`() {
        val chain = fakeChain("https://winatra.supabase.co/rest/v1/config")
        val original = chain.request()
        OfflineCacheInterceptor(isOnline = { true }).intercept(chain)

        val forwarded = chainForwardedRequest(chain)
        assertSame(original, forwarded)
    }

    private fun fakeChain(url: String): Interceptor.Chain {
        val chain = mock(Interceptor.Chain::class.java)
        val request = Request.Builder().url(url).build()
        `when`(chain.request()).thenReturn(request)
        HttpChainStubber.stubProceed(chain) { forwarded ->
            Response.Builder()
                .request(forwarded)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("ok".toResponseBody(plainText))
                .build()
        }
        return chain
    }

    private fun chainForwardedRequest(chain: Interceptor.Chain): Request = HttpChainStubber.forwardedRequest(chain)
}
