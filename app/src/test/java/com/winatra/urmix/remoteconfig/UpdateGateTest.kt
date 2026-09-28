package com.winatra.urmix.remoteconfig

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FASE 6 §7.3: update-gate decision rules (force vs soft vs up-to-date),
 * including the min_extractor_version path and version-compare edge cases.
 */
class UpdateGateTest {

    @Test
    fun `up to date when nothing is newer`() {
        assertEquals(
            UpdateGate.UpdateState.UP_TO_DATE,
            UpdateGate.evaluate("1.0.0", "1.0.0", false, "0.26.5", "0.24.0")
        )
    }

    @Test
    fun `null required versions mean up to date`() {
        assertEquals(
            UpdateGate.UpdateState.UP_TO_DATE,
            UpdateGate.evaluate("1.0.0", null, true, null, null)
        )
    }

    @Test
    fun `outdated app without force is soft update`() {
        assertEquals(
            UpdateGate.UpdateState.SOFT_UPDATE,
            UpdateGate.evaluate("1.0.0", "1.1.0", false, "0.26.5", "0.24.0")
        )
    }

    @Test
    fun `outdated app with force is force update`() {
        assertEquals(
            UpdateGate.UpdateState.FORCE_UPDATE,
            UpdateGate.evaluate("1.0.0", "1.1.0", true, "0.26.5", "0.24.0")
        )
    }

    @Test
    fun `stale extractor triggers soft update when not forced`() {
        assertEquals(
            UpdateGate.UpdateState.SOFT_UPDATE,
            UpdateGate.evaluate("1.0.0", "1.0.0", false, "0.23.0", "0.24.0")
        )
    }

    @Test
    fun `stale extractor triggers force update when forced`() {
        assertEquals(
            UpdateGate.UpdateState.FORCE_UPDATE,
            UpdateGate.evaluate("1.0.0", "1.0.0", true, "0.23.0", "0.24.0")
        )
    }

    @Test
    fun `unknown extractor version never blocks on extractor rule`() {
        assertEquals(
            UpdateGate.UpdateState.UP_TO_DATE,
            UpdateGate.evaluate("1.0.0", "1.0.0", true, null, "0.24.0")
        )
    }

    @Test
    fun `version compare handles lengths and suffixes`() {
        assertTrue(UpdateGate.isNewerVersion("1.0.1", "1.0.0"))
        assertFalse(UpdateGate.isNewerVersion("1.0.0", "1.0.0"))
        assertFalse(UpdateGate.isNewerVersion("1.0.0", "1.0.1"))
        assertFalse(UpdateGate.isNewerVersion("1.0", "1.0.0"))
        assertFalse(UpdateGate.isNewerVersion(null, "1.0.0"))
        assertEquals(0, UpdateGate.compareVersions("1.0.0-beta", "1.0.0"))
        assertEquals(0, UpdateGate.compareVersions("v1.0", "1.0.0"))
        assertTrue(UpdateGate.compareVersions("0.26.5", "0.24.0") > 0)
    }
}
