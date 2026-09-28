package com.winatra.urmix.util

import android.content.Context
import android.util.Log
import coil3.SingletonImageLoader
import com.winatra.urmix.DownloaderImpl

/*
 * CacheCleaner — user-triggered cache wipe for the FASE 5 §12.1 caches.
 *
 * The bounded Coil image caches (memory + disk) and the shared OkHttp response
 * cache are invisible to the user, so they need an explicit way out; otherwise
 * the only options are "clear app data" in the system settings or an uninstall.
 * Every step is best-effort: one failing cache never blocks the other two.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 * This file is part of URMIX, a fork of NewPipe (org.schabi.newpipe).
 */

object CacheCleaner {
    private val TAG = CacheCleaner::class.java.simpleName

    /**
     * Empties the metadata, image (memory + disk) and HTTP response caches.
     *
     * @param context any context; the application context is used internally
     * @return true when every cache was cleared without an error
     */
    @JvmStatic
    fun clearAll(context: Context): Boolean {
        val appContext = context.applicationContext
        val metadataCleared = clearMetadataCache()
        val imagesCleared = clearImageCaches(appContext)
        val httpCleared = clearHttpCache()
        return metadataCleared && imagesCleared && httpCleared
    }

    /** Clears [InfoCache], the in-memory metadata cache of the extractor layer. */
    private fun clearMetadataCache(): Boolean {
        return try {
            InfoCache.getInstance().clearCache()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Could not clear the metadata cache", e)
            false
        }
    }

    /** Clears the Coil memory cache and the Coil disk cache (the image_cache dir). */
    private fun clearImageCaches(context: Context): Boolean {
        return try {
            val imageLoader = SingletonImageLoader.get(context)
            imageLoader.memoryCache?.clear()
            imageLoader.diskCache?.clear()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Could not clear the image caches", e)
            false
        }
    }

    /** Clears the shared OkHttp response cache installed by [DownloaderImpl]. */
    private fun clearHttpCache(): Boolean {
        return try {
            val downloader = DownloaderImpl.getInstance() ?: return true
            downloader.clearHttpCache()
        } catch (e: Exception) {
            Log.w(TAG, "Could not clear the HTTP response cache", e)
            false
        }
    }
}
