package com.winatra.urmix.util.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression guard for the FASE 5 §12.2 banner rules: only real state changes
 * surface to the UI, and online retries are debounced so a flapping network
 * triggers at most one reload per second.
 */
class NetworkStateMachineTest {

    @Test
    fun `first event with the same state is not a change`() {
        val machine = NetworkStateMachine(initialOnline = true)

        assertFalse(machine.onStateChanged(true))
    }

    @Test
    fun `offline transition is a change and tracks the new state`() {
        val machine = NetworkStateMachine(initialOnline = true)

        assertTrue(machine.onStateChanged(false))
        assertFalse(machine.onStateChanged(false))
    }

    @Test
    fun `online retries are debounced`() {
        val machine = NetworkStateMachine(initialOnline = false)

        assertTrue(machine.shouldRetryOnline(10_000L))
        assertFalse(machine.shouldRetryOnline(10_000L + 999L))
        assertTrue(machine.shouldRetryOnline(10_000L + 1_000L))
    }

    @Test
    fun `custom debounce window is honoured`() {
        val machine = NetworkStateMachine(initialOnline = false, debounceMillis = 5_000L)

        assertTrue(machine.shouldRetryOnline(0L))
        assertFalse(machine.shouldRetryOnline(4_999L))
        assertTrue(machine.shouldRetryOnline(5_000L))
    }
}
