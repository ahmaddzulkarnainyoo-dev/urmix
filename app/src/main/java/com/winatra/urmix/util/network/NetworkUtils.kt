package com.winatra.urmix.util.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat

/*
 * NetworkUtils — single source of truth for connectivity state (FASE 5 §12).
 *
 * Replaces the deprecated `ConnectivityManager.getActiveNetworkInfo()` usage
 * (ListHelper.isMeteredNetwork now delegates here) and provides the offline
 * detection required by the HTTP cache interceptors and the offline banners.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 * This file is part of URMIX, a fork of NewPipe (org.schabi.newpipe).
 */

object NetworkUtils {

    /**
     * Pure decision function so the connectivity rules can be unit tested on
     * the JVM. [requireValidation] is only true on API 23+, where the framework
     * actually tracks NET_CAPABILITY_VALIDATED (on older APIs it would always
     * be false and offline detection would break).
     */
    @JvmStatic
    fun isNetworkUsable(
        hasInternetCapability: Boolean,
        isValidated: Boolean,
        requireValidation: Boolean
    ): Boolean = hasInternetCapability && (!requireValidation || isValidated)

    /** True when the current default network can reach the internet. */
    @JvmStatic
    fun isOnline(context: Context): Boolean {
        val manager = ContextCompat.getSystemService(context, ConnectivityManager::class.java)
            ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return isNetworkUsable(
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        )
    }

    /** True when the current default network is metered (mobile data). */
    @JvmStatic
    fun isMetered(context: Context): Boolean {
        val manager = ContextCompat.getSystemService(context, ConnectivityManager::class.java)
            ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    /**
     * Registers a callback for default-network changes and posts them on the
     * main thread. The returned handle must be closed (typically from
     * onDestroyView) to avoid leaking the receiver, or null when connectivity
     * information is unavailable on this device.
     */
    @JvmStatic
    fun registerDefaultNetworkCallback(
        context: Context,
        onAvailable: () -> Unit = {},
        onLost: () -> Unit = {}
    ): AutoCloseable? {
        val manager = ContextCompat.getSystemService(context, ConnectivityManager::class.java)
            ?: return null
        val mainHandler = Handler(Looper.getMainLooper())
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                mainHandler.post { onAvailable() }
            }

            override fun onLost(network: Network) {
                mainHandler.post { onLost() }
            }
        }
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                manager.registerDefaultNetworkCallback(callback)
            } else {
                // API 21-22: no registerDefaultNetworkCallback() yet.
                manager.registerNetworkCallback(
                    NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build(),
                    callback
                )
            }
            AutoCloseable { manager.unregisterNetworkCallback(callback) }
        } catch (e: Exception) {
            // SecurityException or a full callback slot: degrade silently.
            null
        }
    }
}
