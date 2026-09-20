package com.winatra.urmix.network

import android.content.Context
import com.winatra.urmix.util.network.NetworkUtils
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response

/*
 * HttpCacheInterceptors — the two interceptors that make the shared OkHttp
 * response cache work for FASE 5 §12.1 without ever caching extractor traffic.
 *
 * - [HttpCacheControlInterceptor] must be installed with addNetworkInterceptor:
 *   it stamps the response with the policy Cache-Control (so the cache stores
 *   only whitelisted hosts) and pins everything else to `no-store`.
 * - [OfflineCacheInterceptor] must be installed with addInterceptor (it runs
 *   before the cache lookup): while the device is offline it rewrites
 *   whitelisted requests to CacheControl.FORCE_CACHE so they are served from
 *   disk instead of failing with an UnknownHostException.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 * This file is part of URMIX, a fork of NewPipe (org.schabi.newpipe).
 */

/**
 * Enforces [HostCachePolicy] on the shared OkHttp client. Network interceptors
 * see the response of every hop and their edits flow back through the cache
 * interceptor, which is the documented way to rewrite response cache headers.
 */
class HttpCacheControlInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val host = request.url.host

        return if (HostCachePolicy.policyFor(host) == HostCachePolicy.Policy.CACHEABLE) {
            val response = chain.proceed(request)
            response.newBuilder()
                .header("Cache-Control", HostCachePolicy.CACHEABLE_CACHE_CONTROL)
                .removeHeader("Pragma")
                .build()
        } else {
            // Never store (and never look up) extractor / unknown-host payloads.
            val noStoreRequest = request.newBuilder()
                .header("Cache-Control", HostCachePolicy.NO_STORE_CACHE_CONTROL)
                .build()
            val response = chain.proceed(noStoreRequest)
            response.newBuilder()
                .header("Cache-Control", HostCachePolicy.NO_STORE_CACHE_CONTROL)
                .removeHeader("Pragma")
                .build()
        }
    }
}

/**
 * Serves whitelisted requests from the HTTP cache while the device is offline
 * instead of failing with an UnknownHostException. Requests to hosts that are
 * not [HostCachePolicy.Policy.CACHEABLE] pass through untouched, so extractor
 * behaviour offline is unchanged (normal network error handling).
 */
class OfflineCacheInterceptor : Interceptor {

    private val isOnline: () -> Boolean

    constructor(context: Context) : this({ NetworkUtils.isOnline(context) })

    constructor(isOnline: () -> Boolean) {
        this.isOnline = isOnline
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (HostCachePolicy.policyFor(request.url.host) != HostCachePolicy.Policy.CACHEABLE) {
            return chain.proceed(request)
        }
        return if (isOnline()) {
            chain.proceed(request)
        } else {
            chain.proceed(
                request.newBuilder()
                    .cacheControl(CacheControl.FORCE_CACHE)
                    .build()
            )
        }
    }
}
