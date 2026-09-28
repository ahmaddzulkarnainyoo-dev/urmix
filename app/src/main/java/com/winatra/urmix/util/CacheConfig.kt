package com.winatra.urmix.util

/*
 * CacheConfig — central, bounded cache sizing for FASE 5 §12.1.
 *
 * Single source of truth for the Coil image caches (memory + disk) and for the
 * shared OkHttp response cache that backs both the NewPipe extractor and
 * Coil's network layer. All sizes are deliberately conservative so that
 * caching can never push URMIX out of memory on low-RAM devices.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 * This file is part of URMIX, a fork of NewPipe (org.schabi.newpipe).
 */

object CacheConfig {

    /** Subdirectory of the app cache dir where Coil stores image responses. */
    const val IMAGE_CACHE_DIR = "image_cache"

    /** Subdirectory of the app cache dir where OkHttp stores HTTP responses. */
    const val HTTP_CACHE_DIR = "http_cache"

    private const val BYTES_PER_MIB = 1024L * 1024L

    /** Coil disk cache: 256 MiB normally, 128 MiB on low-RAM devices. */
    private const val IMAGE_DISK_CACHE_MIB = 256L
    private const val IMAGE_DISK_CACHE_LOW_RAM_MIB = 128L

    /** Shared OkHttp response cache: 64 MiB normally, 32 MiB on low-RAM devices. */
    private const val HTTP_CACHE_MIB = 64L
    private const val HTTP_CACHE_LOW_RAM_MIB = 32L

    /** Coil memory cache share of the total app memory. */
    private const val MEMORY_CACHE_PERCENT_NORMAL = 0.25
    private const val MEMORY_CACHE_PERCENT_LOW_RAM = 0.12

    /** Coil disk cache size in bytes. */
    @JvmStatic
    fun imageDiskCacheBytes(isLowRamDevice: Boolean): Long = (if (isLowRamDevice) IMAGE_DISK_CACHE_LOW_RAM_MIB else IMAGE_DISK_CACHE_MIB) * BYTES_PER_MIB

    /** Shared OkHttp response cache size in bytes. */
    @JvmStatic
    fun httpCacheBytes(isLowRamDevice: Boolean): Long = (if (isLowRamDevice) HTTP_CACHE_LOW_RAM_MIB else HTTP_CACHE_MIB) * BYTES_PER_MIB

    /** Coil memory cache share of the total app memory. */
    @JvmStatic
    fun memoryCachePercent(isLowRamDevice: Boolean): Double = if (isLowRamDevice) MEMORY_CACHE_PERCENT_LOW_RAM else MEMORY_CACHE_PERCENT_NORMAL
}
