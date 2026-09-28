package com.winatra.urmix.remoteconfig

/**
 * FASE 6 §7.3: pure update-gate decision logic for the WINATRA update loop.
 *
 * Extracted from [RemoteConfigRepository] so the force/soft/up-to-date rules
 * are unit-testable on the JVM without Android dependencies:
 * - force update blocks startup (non-dismissable dialog in SplashActivity),
 * - soft update never blocks (dismissible banner, app keeps running),
 * - stale extractor triggers the same path as an outdated app version.
 */
object UpdateGate {

    /**
     * Possible outcomes of evaluating a cached remote config.
     */
    enum class UpdateState {
        /** App is current; show announcement (if any) and continue normally. */
        UP_TO_DATE,

        /** Update available but optional; show a dismissible banner only. */
        SOFT_UPDATE,

        /** Update required; block startup with the force-update dialog. */
        FORCE_UPDATE
    }

    /**
     * Evaluates the update state for the given versions.
     *
     * @param installedAppVersion versionName of the installed app (e.g. BuildConfig.VERSION_NAME).
     * @param requiredAppVersion app_version from remote config, or null when absent.
     * @param forceUpdate force_update flag from remote config.
     * @param installedExtractorVersion local extractor version, or null when unknown.
     * @param minExtractorVersion min_extractor_version from remote config, or null when absent.
     */
    fun evaluate(
        installedAppVersion: String,
        requiredAppVersion: String?,
        forceUpdate: Boolean,
        installedExtractorVersion: String?,
        minExtractorVersion: String?
    ): UpdateState {
        val appOutdated = isNewerVersion(requiredAppVersion, installedAppVersion)
        val extractorOutdated = installedExtractorVersion != null &&
            isNewerVersion(minExtractorVersion, installedExtractorVersion)
        if (!appOutdated && !extractorOutdated) {
            return UpdateState.UP_TO_DATE
        }
        return if (forceUpdate) UpdateState.FORCE_UPDATE else UpdateState.SOFT_UPDATE
    }

    /**
     * Returns true when [required] names a version strictly newer than [current].
     */
    fun isNewerVersion(required: String?, current: String): Boolean {
        if (required == null) {
            return false
        }
        return compareVersions(required, current) > 0
    }

    /**
     * Compares dotted version strings numerically, tolerating different lengths
     * and non-numeric prefixes/suffixes (e.g. "v1.0" and "1.0.0-beta" compare
     * as 1.0 and 1.0.0 respectively).
     */
    fun compareVersions(a: String, b: String): Int {
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
        val trimmed = segment.trim().dropWhile { !it.isDigit() }
        val numericPrefix = trimmed.takeWhile { it.isDigit() }
        if (numericPrefix.isEmpty()) {
            return 0
        }
        return try {
            numericPrefix.toInt()
        } catch (e: NumberFormatException) {
            0
        }
    }
}
