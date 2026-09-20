package com.winatra.urmix.util.network

import android.content.Context
import android.util.Log
import android.view.View

/*
 * NetworkStateObserver — fragment-scoped connectivity listener (FASE 5 §12.2).
 *
 * Wraps NetworkUtils.registerDefaultNetworkCallback and adds the behaviour the
 * offline banners need: an initial isOnline() verdict, a debounce so a flapping
 * connection triggers at most one retry, and an AutoCloseable handle that MUST
 * be closed from onDestroyView to avoid leaking the callback.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 * This file is part of URMIX, a fork of NewPipe (org.schabi.newpipe).
 */

class NetworkStateObserver @JvmOverloads constructor(
    context: Context,
    private val onOnline: Runnable = Runnable {},
    private val onOffline: Runnable = Runnable {}
) {
    companion object {
        private const val TAG = "NetworkStateObserver"
        private const val RETRY_DEBOUNCE_MILLIS = 1000L
    }

    private val appContext: Context = context.applicationContext
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    /** Gates the first onAvailable event so we don't retry while already online. */
    private var lastKnownOnline: Boolean = NetworkUtils.isOnline(appContext)

    private var lastRetryAtMillis: Long = 0L

    /** Non-null while registered; close() from onDestroyView unregisters. */
    var handle: AutoCloseable? = null
        private set

    val isOnline: Boolean
        get() = lastKnownOnline

    /** Registers the underlying callback. Safe to call more than once. */
    fun start() {
        if (handle != null) {
            return
        }
        handle = NetworkUtils.registerDefaultNetworkCallback(
            appContext,
            onAvailable = { postStateChange(true) },
            onLost = { postStateChange(false) }
        )
    }

    /** Unregisters the callback; call from onDestroyView. Idempotent. */
    fun stop() {
        handle?.let {
            try {
                it.close()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to unregister network callback", e)
            }
        }
        handle = null
    }

    /**
     * Re-evaluates connectivity and refreshes [anchor] visibility. Returns the
     * current verdict so callers can branch their loading strategy.
     */
    fun refresh(anchor: View?): Boolean {
        val online = NetworkUtils.isOnline(appContext)
        lastKnownOnline = online
        anchor?.visibility = if (online) View.GONE else View.VISIBLE
        return online
    }

    private fun postStateChange(online: Boolean) {
        mainHandler.post {
            val changed = online != lastKnownOnline
            lastKnownOnline = online
            if (!changed) {
                return@post
            }
            if (online) {
                val now = android.os.SystemClock.uptimeMillis()
                if (now - lastRetryAtMillis >= RETRY_DEBOUNCE_MILLIS) {
                    lastRetryAtMillis = now
                    onOnline.run()
                }
            } else {
                onOffline.run()
            }
        }
    }
}
