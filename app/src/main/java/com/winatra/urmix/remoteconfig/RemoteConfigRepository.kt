package com.winatra.urmix.remoteconfig

/*
 * RemoteConfigRepository — WINATRA Remote Control Config (blueprint v2 §1.2).
 *
 * Fetches the remote config from Supabase with a strict 4 s timeout, caches the
 * last successful payload in a dedicated SharedPreferences store and fails back
 * silently. App startup is never blocked by network problems; it is only blocked
 * when a *cached* config demands a forced update (force_update == true) and the
 * locally installed app version is older than the required one.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 * This file is part of URMIX, a fork of NewPipe (org.schabi.newpipe).
 */

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.grack.nanojson.JsonObject
import com.grack.nanojson.JsonParser
import com.winatra.urmix.BuildConfig
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import okhttp3.OkHttpClient
import okhttp3.Request

object RemoteConfigRepository {

    private const val TAG = "RemoteConfigRepository"

    /**
     * Supabase REST endpoint which serves the WINATRA remote config.
     * TODO(WINATRA): replace with the real Supabase endpoint (+ apikey header)
     * before the first public release.
     */
    const val REMOTE_CONFIG_URL = "https://winatra.supabase.co/rest/v1/urmix_config?select=*"

    /** Fallback URL used when the config does not provide an update_url. */
    const val DEFAULT_UPDATE_URL = "https://winatra.com/urmix/download"

    private const val PREFS_NAME = "urmix_remote_config"
    private const val KEY_CACHED_JSON = "cached_config_json"
    private const val KEY_LAST_FETCH_MILLIS = "last_successful_fetch_millis"
    private const val TIMEOUT_SECONDS = 4L

    private val cachedConfig = AtomicReference<RemoteConfig?>(null)

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    data class Announcement(
        val active: Boolean,
        val title: String?,
        val message: String?
    )

    data class Donation(
        val title: String?,
        val saweriaUrl: String?,
        val qrisUrl: String?,
        val dailyMessage: String?
    )

    data class RemoteConfig(
        val configSchemaVersion: Int,
        val appVersion: String?,
        val minExtractorVersion: String?,
        val forceUpdate: Boolean,
        val updateUrl: String?,
        val announcement: Announcement?,
        val donation: Donation?,
        val podcastChannels: List<String> = emptyList(),
        val podcastPlaylists: List<String> = emptyList()
    ) {
        /** All curated podcast source URLs (channels first, then playlists). */
        val podcastSources: List<String>
            get() = podcastChannels + podcastPlaylists
    }

    /** Returns the in-memory config, transparently loading the persisted cache. */
    fun getCachedConfig(context: Context): RemoteConfig? {
        val inMemory = cachedConfig.get()
        if (inMemory != null) {
            return inMemory
        }
        return readCache(context)
    }

    /**
     * True only when a *cached* config demands a blocking forced update and the
     * installed app version is older than the required app_version.
     */
    fun shouldBlockStartup(context: Context): Boolean {
        val config = getCachedConfig(context) ?: return false
        if (!config.forceUpdate) {
            return false
        }
        return isNewerVersion(config.appVersion, BuildConfig.VERSION_NAME)
    }

    /** The update_url to direct the user to (config value or the WINATRA default). */
    fun getEffectiveUpdateUrl(context: Context): String = getCachedConfig(context)?.updateUrl ?: DEFAULT_UPDATE_URL

    /** Epoch millis of the last successful remote fetch, or 0 when never fetched. */
    fun getLastSuccessfulFetchMillis(context: Context): Long =
        prefs(context).getLong(KEY_LAST_FETCH_MILLIS, 0L)

    /**
     * True when the cached config is missing or older than [maxAgeMillis].
     * Used by FASE 5 §12.2 to decide whether an offline banner must mention a
     * potentially outdated remote config.
     */
    fun isConfigStale(context: Context, maxAgeMillis: Long): Boolean {
        val lastFetch = getLastSuccessfulFetchMillis(context)
        return lastFetch == 0L || System.currentTimeMillis() - lastFetch > maxAgeMillis
    }

    /**
     * Fetches the remote config with a strict 4 s timeout and caches the last
     * successful response. Offline/timeout/corrupt payloads fall back to the
     * cached config without disturbing the user.
     */
    fun refresh(context: Context) {
        // Might block up to ~4 s: only ever call from a background thread.
        val raw = try {
            fetchConfigJson()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Remote config fetch failed; using cached config", e)
            }
            null
        }
        if (raw == null) {
            return
        }

        val config = parseConfig(raw) ?: return // corrupt JSON -> ignore, keep cache
        cachedConfig.set(config)
        prefs(context)
            .edit()
            .putString(KEY_CACHED_JSON, raw)
            .putLong(KEY_LAST_FETCH_MILLIS, System.currentTimeMillis())
            .apply()
    }

    private fun fetchConfigJson(): String? {
        val request = Request.Builder().url(REMOTE_CONFIG_URL).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                if (BuildConfig.DEBUG) {
                    Log.w(TAG, "Remote config request failed: HTTP " + response.code)
                }
                return null
            }
            return response.body.string()
        }
    }

    private fun parseConfig(raw: String): RemoteConfig? {
        return try {
            val root: JsonObject = JsonParser.`object`().from(raw)
            val announcementObject = root.getObject("announcement")
            val donationObject = root.getObject("donation")

            val announcement = if (announcementObject == null) {
                null
            } else {
                Announcement(
                    announcementObject.getBoolean("active", true),
                    nullable(announcementObject.getString("title")),
                    nullable(announcementObject.getString("message"))
                )
            }

            val donation = if (donationObject == null) {
                null
            } else {
                Donation(
                    nullable(donationObject.getString("title")),
                    nullable(donationObject.getString("saweria_url")),
                    nullable(donationObject.getString("qris_url")),
                    nullable(donationObject.getString("daily_message"))
                )
            }

            RemoteConfig(
                root.getInt("config_schema_version", 0),
                nullable(root.getString("app_version")),
                nullable(root.getString("min_extractor_version")),
                root.getBoolean("force_update", false),
                validateHttpUrl(nullable(root.getString("update_url"))),
                announcement,
                donation,
                parseStringList(root, "podcast_channels"),
                parseStringList(root, "podcast_playlists")
            )
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Remote config payload was corrupt; ignoring it", e)
            }
            null
        }
    }

    private fun readCache(context: Context): RemoteConfig? {
        val json = prefs(context).getString(KEY_CACHED_JSON, null) ?: return null
        val parsed = parseConfig(json) ?: return null
        cachedConfig.set(parsed)
        return parsed
    }

    private fun prefs(context: Context): SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Parses a JSON string array into validated http(s) URLs (blueprint v2 §3.3). */
    private fun parseStringList(root: JsonObject, key: String): List<String> {
        return try {
            if (!root.containsKey(key)) {
                return emptyList()
            }
            val array = root.getArray(key) ?: return emptyList()
            val result = ArrayList<String>(array.size)
            for (i in 0 until array.size) {
                val raw = array.getString(i, null)
                val url = validateHttpUrl(nullable(raw))
                if (url != null) {
                    result.add(url)
                }
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun nullable(value: String?): String? = if (value.isNullOrEmpty()) null else value

    private fun validateHttpUrl(url: String?): String? = if (url != null && (url.startsWith("https://") || url.startsWith("http://"))) {
        url
    } else {
        null
    }

    private fun isNewerVersion(required: String?, current: String): Boolean {
        if (required == null) {
            return false
        }
        return compareVersions(required, current) > 0
    }

    private fun compareVersions(a: String, b: String): Int {
        val partsA = a.split(".")
        val partsB = b.split(".")
        val length = maxOf(partsA.size, partsB.size)
        for (i in 0 until length) {
            val partA = parseSegment(partsA.getOrNull(i))
            val partB = parseSegment(partsB.getOrNull(i))
            if (partA != partB) {
                return if (partA > partB) 1 else -1
            }
        }
        return 0
    }

    private fun parseSegment(segment: String?): Int {
        if (segment == null) {
            return 0
        }
        return try {
            segment.trim().toInt()
        } catch (e: NumberFormatException) {
            0
        }
    }
}
