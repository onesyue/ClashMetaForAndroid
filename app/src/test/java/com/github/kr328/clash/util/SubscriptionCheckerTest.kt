package com.github.kr328.clash.util

import org.junit.Assert.*
import org.junit.Test

class SubscriptionCheckerTest {

    @Test
    fun `daysLeft greater than 7 returns no alert`() {
        assertNull(SubscriptionChecker.computeAlertLevel(8))
        assertNull(SubscriptionChecker.computeAlertLevel(30))
        assertNull(SubscriptionChecker.computeAlertLevel(365))
    }

    @Test
    fun `daysLeft equals 7 returns level 7`() {
        assertEquals(7, SubscriptionChecker.computeAlertLevel(7))
    }

    @Test
    fun `daysLeft equals 5 returns level 7`() {
        assertEquals(7, SubscriptionChecker.computeAlertLevel(5))
    }

    @Test
    fun `daysLeft equals 4 returns level 7`() {
        assertEquals(7, SubscriptionChecker.computeAlertLevel(4))
    }

    @Test
    fun `daysLeft equals 3 returns level 3`() {
        assertEquals(3, SubscriptionChecker.computeAlertLevel(3))
    }

    @Test
    fun `daysLeft equals 2 returns level 3`() {
        assertEquals(3, SubscriptionChecker.computeAlertLevel(2))
    }

    @Test
    fun `daysLeft equals 1 returns level 1`() {
        assertEquals(1, SubscriptionChecker.computeAlertLevel(1))
    }

    @Test
    fun `daysLeft equals 0 returns level 1`() {
        assertEquals(1, SubscriptionChecker.computeAlertLevel(0))
    }

    @Test
    fun `daysLeft negative returns level 0 (expired)`() {
        assertEquals(0, SubscriptionChecker.computeAlertLevel(-1))
        assertEquals(0, SubscriptionChecker.computeAlertLevel(-100))
    }
}
