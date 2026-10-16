package com.hostelcare.app

import com.hostelcare.app.data.model.Role
import com.hostelcare.app.ui.navigation.Routes
import com.hostelcare.app.ui.screens.auth.SPLASH_DURATION_MS
import com.hostelcare.app.ui.screens.auth.getDestinationForRole
import org.junit.Assert.assertEquals
import org.junit.Test

class SplashTimingTest {

    @Test
    fun testSplashDurationIsExactlyFiveSeconds() {
        assertEquals(5000L, SPLASH_DURATION_MS)
    }

    @Test
    fun testRemainingTimeCalculation() {
        // Initial start
        val elapsedZero = 0L
        val remainingAtStart = (SPLASH_DURATION_MS - elapsedZero).coerceAtLeast(0L)
        assertEquals(5000L, remainingAtStart)

        // After 2.5 seconds
        val elapsedHalf = 2500L
        val remainingAtHalf = (SPLASH_DURATION_MS - elapsedHalf).coerceAtLeast(0L)
        assertEquals(2500L, remainingAtHalf)

        // After exactly 5 seconds
        val elapsedExact = 5000L
        val remainingAtExact = (SPLASH_DURATION_MS - elapsedExact).coerceAtLeast(0L)
        assertEquals(0L, remainingAtExact)

        // If elapsed exceeds 5 seconds (e.g. slow resume or rotation after long idle)
        val elapsedOver = 6200L
        val remainingOver = (SPLASH_DURATION_MS - elapsedOver).coerceAtLeast(0L)
        assertEquals(0L, remainingOver)
    }

    @Test
    fun testStartupNavigationDestinations() {
        // Unauthenticated user -> Portal Selection
        assertEquals(Routes.PORTAL_SELECTION, getDestinationForRole(null))

        // Student -> Student Home
        assertEquals(Routes.STUDENT_HOME, getDestinationForRole(Role.STUDENT))

        // Warden -> Warden Home
        assertEquals(Routes.WARDEN_HOME, getDestinationForRole(Role.WARDEN))

        // Staff -> Staff Home
        assertEquals(Routes.STAFF_HOME, getDestinationForRole(Role.STAFF))
    }
}
