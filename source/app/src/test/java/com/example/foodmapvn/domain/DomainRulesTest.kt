package com.example.foodmapvn.domain

import com.example.foodmapvn.data.model.BadgeStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainRulesTest {

    private val rules = BusinessRules()

    // Toạ độ gần chợ Đông Ba, Huế (dùng để thử)
    private val placeLat = 16.4720
    private val placeLon = 107.5880

    @Test
    fun checkIn_withinRadius_isValid() {
        val result = CheckInValidator(rules).validate(16.4725, 107.5882, placeLat, placeLon, null)
        assertTrue(result is CheckInResult.Valid)
    }

    @Test
    fun checkIn_farAway_isTooFar() {
        val result = CheckInValidator(rules).validate(16.4800, 107.5880, placeLat, placeLon, null)
        assertTrue(result is CheckInResult.TooFar)
    }

    @Test
    fun checkIn_twiceWithin12Hours_isTooSoon() {
        val now = 1_000_000_000L
        val result = CheckInValidator(rules).validate(placeLat, placeLon, placeLat, placeLon, now - 3_600_000L, now)
        assertTrue(result is CheckInResult.TooSoon)
    }

    @Test
    fun priceRange_usesQuartiles() {
        val range = PriceRangeCalculator(rules).priceRange(listOf(30_000, 35_000, 40_000, 45_000, 50_000))!!
        assertEquals(35_000L, range.low)
        assertEquals(45_000L, range.high)
    }

    @Test
    fun overpriced_whenAbove20Percent() {
        val calc = PriceRangeCalculator(rules)
        val range = PriceRange(35_000, 50_000)
        assertFalse(calc.isOverpriced(60_000, range))
        assertTrue(calc.isOverpriced(61_000, range))
    }

    @Test
    fun trustScore_perfectPlace_is100() {
        val reviews = List(10) { ReviewInput(stars = 5, isLocal = false) }
        assertEquals(100, TrustScoreCalculator(rules).calculate(reviews, 20, false, 0))
    }

    @Test
    fun trustScore_isClampedToZero() {
        val reviews = listOf(ReviewInput(1, false))
        assertEquals(0, TrustScoreCalculator(rules).calculate(reviews, 0, true, 10))
    }

    @Test
    fun localReview_hasMoreWeight() {
        val avg = TrustScoreCalculator(rules).weightedAverageStars(
            listOf(ReviewInput(5, isLocal = true), ReviewInput(3, isLocal = false))
        )!!
        assertEquals(4.2, avg, 0.001) // (5×1,5 + 3×1) / 2,5
    }

    @Test
    fun badge_progress() {
        val calc = BadgeCalculator(rules)
        assertEquals(BadgeStatus.LOCKED, calc.progress(0, 0).status)
        assertEquals(BadgeStatus.IN_PROGRESS, calc.progress(1, 1).status)
        assertEquals(BadgeStatus.ACHIEVED, calc.progress(5, 3).status)
    }

    @Test
    fun formatters() {
        assertEquals("35.000", formatVnd(35_000))
        assertEquals("1,4 km", GeoUtils.formatDistance(1_400.0))
        assertEquals("350 m", GeoUtils.formatDistance(350.0))
    }
}
