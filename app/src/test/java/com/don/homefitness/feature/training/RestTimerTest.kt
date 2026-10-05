package com.don.homefitness.feature.training

import org.junit.Assert.assertEquals
import org.junit.Test

class RestTimerTest {
    private val state = RestState("boot-a", 90_000L, 90)

    @Test fun `elapsed time returns remaining seconds`() { assertEquals(25, RestTimer.remainingSeconds(state, 65_000L, "boot-a")) }
    @Test fun `wall clock changes do not affect elapsed result`() { assertEquals(25, RestTimer.remainingSeconds(state, 65_000L, "boot-a")) }
    @Test fun `deadline and reboot return zero`() {
        assertEquals(0, RestTimer.remainingSeconds(state, 90_000L, "boot-a"))
        assertEquals(0, RestTimer.remainingSeconds(state, 65_000L, "boot-b"))
    }
}
