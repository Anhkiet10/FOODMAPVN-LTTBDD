package com.example.foodmapvn.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrustScoreCalculatorTest {

    private val rules = BusinessRules()
    private val calculator = TrustScoreCalculator(rules)

    @Test
    fun calculate_noReviews_checkInOnly() {
        // 10 checkins = 50% saturation (0.5), no price warning = 1.0 price part
        // Score = 100 * (0.5*0 + 0.3*0.5 + 0.2*1.0) = 35
        val score = calculator.calculate(emptyList(), validCheckIns90Days = 10, hasPriceWarning = false, confirmedViolations = 0)
        assertEquals(35, score)
    }

    @Test
    fun calculate_highRating_withCheckIns_perfectScore() {
        val reviews = List(5) { ReviewInput(stars = 5, isLocal = true) }
        val score = calculator.calculate(reviews, validCheckIns90Days = 20, hasPriceWarning = false, confirmedViolations = 0)
        assertEquals(100, score)
    }

    @Test
    fun calculate_priceWarning_reducesScore() {
        val reviews = List(5) { ReviewInput(stars = 5, isLocal = true) }
        val scoreWithWarning = calculator.calculate(reviews, validCheckIns90Days = 20, hasPriceWarning = true, confirmedViolations = 0)
        // Score = 100 * (0.5*1.0 + 0.3*1.0 + 0.2*0.0) = 80
        assertEquals(80, scoreWithWarning)
    }

    @Test
    fun calculate_penalizedByViolations() {
        val reviews = List(5) { ReviewInput(stars = 5, isLocal = true) }
        val scoreWithViolations = calculator.calculate(reviews, validCheckIns90Days = 20, hasPriceWarning = false, confirmedViolations = 2)
        // 100 - (5 * 2) = 90
        assertEquals(90, scoreWithViolations)
    }

    @Test
    fun isVerified_meetsThresholds_returnsTrue() {
        // Requires >= 5 checkins and >= 3 distinct users
        assertTrue(calculator.isVerified(validCheckIns = 5, distinctUsers = 3))
    }

    @Test
    fun isVerified_belowThreshold_returnsFalse() {
        assertFalse(calculator.isVerified(validCheckIns = 4, distinctUsers = 3))
        assertFalse(calculator.isVerified(validCheckIns = 5, distinctUsers = 2))
    }
}
