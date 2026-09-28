package com.winatra.urmix.donation

import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FASE 6 §6: daily donation prompt rules — max 1x per day, dismissible,
 * suppressed without config or without notification permission.
 */
class DonationPromptGateTest {
    private val today = LocalDate.of(2026, 9, 28)

    @Test
    fun `first run with config shows prompt`() {
        assertTrue(DonationPromptGate.shouldShow(true, true, null, today))
    }

    @Test
    fun `same day second run is suppressed`() {
        assertFalse(
            DonationPromptGate.shouldShow(true, true, today.toString(), today)
        )
    }

    @Test
    fun `next day shows again`() {
        assertTrue(
            DonationPromptGate.shouldShow(true, true, today.minusDays(1).toString(), today)
        )
    }

    @Test
    fun `missing donation config suppresses`() {
        assertFalse(DonationPromptGate.shouldShow(false, true, null, today))
    }

    @Test
    fun `disabled notifications suppress`() {
        assertFalse(DonationPromptGate.shouldShow(true, false, null, today))
    }

    @Test
    fun `corrupt stored date shows prompt`() {
        assertTrue(DonationPromptGate.shouldShow(true, true, "not-a-date", today))
    }
}
