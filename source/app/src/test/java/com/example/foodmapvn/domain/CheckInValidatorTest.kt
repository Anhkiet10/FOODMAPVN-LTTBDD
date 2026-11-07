package com.example.foodmapvn.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckInValidatorTest {

    private val rules = BusinessRules(
        checkInRadiusMeters = 100.0,
        checkInCooldownHours = 12,
        reviewCheckInWindowDays = 7,
    )
    private val validator = CheckInValidator(rules)

    private val placeLat = 16.4655
    private val placeLon = 107.5905

    @Test
    fun checkIn_exactSameLocation_isValid() {
        val result = validator.validate(placeLat, placeLon, placeLat, placeLon, null)
        assertTrue(result is CheckInResult.Valid)
        assertEquals(0.0, (result as CheckInResult.Valid).distanceMeters, 0.01)
    }

    @Test
    fun checkIn_50MetersAway_isValid() {
        // ~50m offset in latitude
        val userLat = placeLat + 0.00045
        val result = validator.validate(userLat, placeLon, placeLat, placeLon, null)
        assertTrue(result is CheckInResult.Valid)
        assertTrue((result as CheckInResult.Valid).distanceMeters <= 100.0)
    }

    @Test
    fun checkIn_200MetersAway_returnsTooFar() {
        val userLat = placeLat + 0.0018
        val result = validator.validate(userLat, placeLon, placeLat, placeLon, null)
        assertTrue(result is CheckInResult.TooFar)
        assertEquals(100.0, (result as CheckInResult.TooFar).maxMeters, 0.01)
    }

    @Test
    fun checkIn_withinCooldown_returnsTooSoon() {
        val now = System.currentTimeMillis()
        val lastCheckIn = now - (6 * 3_600_000L) // 6 hours ago
        val result = validator.validate(placeLat, placeLon, placeLat, placeLon, lastCheckIn, now)
        assertTrue(result is CheckInResult.TooSoon)
    }

    @Test
    fun checkIn_afterCooldown_isValid() {
        val now = System.currentTimeMillis()
        val lastCheckIn = now - (13 * 3_600_000L) // 13 hours ago
        val result = validator.validate(placeLat, placeLon, placeLat, placeLon, lastCheckIn, now)
        assertTrue(result is CheckInResult.Valid)
    }

    @Test
    fun reviewHasCheckIn_within7Days_returnsTrue() {
        val now = System.currentTimeMillis()
        val lastCheckIn = now - (3 * 86_400_000L) // 3 days ago
        assertTrue(validator.reviewHasCheckIn(lastCheckIn, now))
    }

    @Test
    fun reviewHasCheckIn_after7Days_returnsFalse() {
        val now = System.currentTimeMillis()
        val lastCheckIn = now - (10 * 86_400_000L) // 10 days ago
        assertFalse(validator.reviewHasCheckIn(lastCheckIn, now))
    }
}
