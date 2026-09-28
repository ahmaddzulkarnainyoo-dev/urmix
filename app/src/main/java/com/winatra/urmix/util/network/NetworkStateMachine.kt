package com.winatra.urmix.util.network

/*
 * NetworkStateMachine — pure gating behind NetworkStateObserver (FASE 5 §12).
 *
 * Decides whether a connectivity change must surface to the UI and whether an
 * online event may trigger a reload. Debounce uses a caller-supplied clock so
 * the rules stay deterministic JVM unit tests.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 * This file is part of URMIX, a fork of NewPipe (org.schabi.newpipe).
 */

/** Minimum gap between two online retries (1s, matches the observer). */
private const val DEFAULT_RETRY_DEBOUNCE_MILLIS = 1000L

/** Pure, deterministic debounce gate for the offline-banner retry logic. */
class NetworkStateMachine(
    initialOnline: Boolean,
    private val debounceMillis: Long = DEFAULT_RETRY_DEBOUNCE_MILLIS
) {
    companion object {
        /** Minimum gap between two online retries (1s, matches the observer). */
        @JvmStatic
        val RETRY_DEBOUNCE_MILLIS: Long = DEFAULT_RETRY_DEBOUNCE_MILLIS
    }

    /** Last connectivity verdict; mirrors what the banner currently shows. */
    var lastKnownOnline: Boolean = initialOnline
        private set

    /** Timestamp of the last fired retry, offset so the first retry is never blocked. */
    private var lastRetryAtMillis: Long = -debounceMillis

    /**
     * Feeds a connectivity event. Returns true only when the state actually
     * changed; [lastKnownOnline] always tracks the latest event.
     */
    fun onStateChanged(online: Boolean): Boolean {
        val changed = online != lastKnownOnline
        lastKnownOnline = online
        return changed
    }

    /**
     * Returns true when an online retry may fire now (state changed + debounce
     * elapsed). Records the firing time so flapping networks retry at most
     * once per [debounceMillis].
     */
    fun shouldRetryOnline(nowMillis: Long): Boolean {
        if (nowMillis - lastRetryAtMillis < debounceMillis) {
            return false
        }
        lastRetryAtMillis = nowMillis
        return true
    }
}
