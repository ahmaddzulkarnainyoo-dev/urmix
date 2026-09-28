package com.winatra.urmix.donation

import java.time.Clock
import java.time.LocalDate

/**
 * FASE 6 §6: pure "max 1x per day" gate for the daily donation prompt.
 *
 * Keeps Android/SharedPreferences access outside so the rule is unit-testable
 * on the JVM. Callers persist [LocalDate.toString] of the day the prompt was
 * shown and pass it back as [lastPromptDateIso].
 */
object DonationPromptGate {
    /**
     * @param donationAvailable false when there is no donation config to show.
     * @param notificationsEnabled false when the user disabled notifications.
     * @param lastPromptDateIso ISO date of the last shown prompt, or null.
     * @param today the current date (injectable clock for tests).
     * @return true when the prompt may be shown now.
     */
    fun shouldShow(
        donationAvailable: Boolean,
        notificationsEnabled: Boolean,
        lastPromptDateIso: String?,
        today: LocalDate = LocalDate.now(Clock.systemDefaultZone())
    ): Boolean {
        if (!donationAvailable || !notificationsEnabled) {
            return false
        }
        if (lastPromptDateIso == null) {
            return true
        }
        val last = runCatching { LocalDate.parse(lastPromptDateIso) }.getOrNull() ?: return true
        return last.isBefore(today)
    }
}
