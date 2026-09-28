package com.winatra.urmix.network

/*
 * HostCachePolicy — decides how the shared OkHttp response cache (FASE 5 §12.1)
 * must treat a given request host.
 *
 * The OkHttp client is shared between the NewPipe extractor and Coil's network
 * fetcher. Serving a stale extractor payload would hand the player expired
 * stream URLs, so caching is strictly opt-in: only URMIX-owned endpoints are
 * cached, extractor hosts are pinned to `no-store`, and unknown hosts default
 * to `no-store` as well (conservative policy, approved for FASE 5 §12.1).
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 * This file is part of URMIX, a fork of NewPipe (org.schabi.newpipe).
 */

object HostCachePolicy {

    /** How the shared HTTP cache must treat responses from a host. */
    enum class Policy {
        /** Responses may be stored and reused by the shared HTTP cache. */
        CACHEABLE,

        /** Responses must never be stored (extractor + unknown hosts). */
        NO_STORE
    }

    /** Hosts whose responses may be stored and reused by the shared HTTP cache. */
    private val CACHEABLE_HOSTS = setOf("winatra.supabase.co")

    /** Extractor / CDN hosts that must always bypass the shared HTTP cache. */
    private val NO_STORE_HOSTS = setOf(
        "youtube.com",
        "youtu.be",
        "youtube-nocookie.com",
        "googlevideo.com",
        "ytimg.com",
        "ggpht.com",
        "soundcloud.com",
        "sndcdn.com",
        "bandcamp.com",
        "media.ccc.de",
        "bittube.video",
        "peertube.tv"
    )

    /** `Cache-Control` forced onto [Policy.CACHEABLE] responses. */
    const val CACHEABLE_CACHE_CONTROL = "public, max-age=300, stale-while-revalidate=259200"

    /** `Cache-Control` forced onto [Policy.NO_STORE] requests and responses. */
    const val NO_STORE_CACHE_CONTROL = "no-store"

    /** Classifies [host] (null-safe, subdomain-aware). */
    @JvmStatic
    fun policyFor(host: String?): Policy = if (host != null && matchesAny(host, CACHEABLE_HOSTS)) {
        Policy.CACHEABLE
    } else {
        // NO_STORE_HOSTS and every unknown host: never store.
        Policy.NO_STORE
    }

    /** True when [host] belongs to a known extractor/CDN host (diagnostics). */
    @JvmStatic
    fun isExtractorHost(host: String?): Boolean = host != null && matchesAny(host, NO_STORE_HOSTS)

    private fun matchesAny(host: String, domains: Set<String>): Boolean = domains.any { domain -> host == domain || host.endsWith(".$domain") }
}
