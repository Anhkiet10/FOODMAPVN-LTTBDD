package com.example.foodmapvn.domain

data class ReviewInput(val stars: Int, val isLocal: Boolean)

/**
 * BR-08: điểm uy tín 0–100 của quán.
 * Uy tín = 100 × (0,5 × (s̄ − 1)/4 + 0,3 × min(1, checkIn/20) + 0,2 × giáHợpLý) − 5 × vi phạm
 */
class TrustScoreCalculator(private val rules: BusinessRules = BusinessRules()) {

    fun calculate(
        reviews: List<ReviewInput>,
        validCheckIns90Days: Int,
        hasPriceWarning: Boolean,
        confirmedViolations: Int,
    ): Int {
        val starPart = weightedAverageStars(reviews)?.let { (it - 1) / 4 } ?: 0.0
        val checkInPart = minOf(1.0, validCheckIns90Days.toDouble() / rules.trustCheckInSaturation)
        val pricePart = if (hasPriceWarning) 0.0 else 1.0
        val score = 100 * (0.5 * starPart + 0.3 * checkInPart + 0.2 * pricePart) -
            rules.trustPenaltyPerViolation * confirmedViolations
        return score.toInt().coerceIn(0, 100)
    }

    /** Trung bình số sao, đánh giá của người địa phương có trọng số cao hơn (BR-10). */
    fun weightedAverageStars(reviews: List<ReviewInput>): Double? {
        if (reviews.isEmpty()) return null
        var sum = 0.0
        var weight = 0.0
        for (r in reviews) {
            val w = if (r.isLocal) rules.localReviewWeight else 1.0
            sum += r.stars.coerceIn(1, 5) * w
            weight += w
        }
        return sum / weight
    }

    /** BR-05: quán đã xác thực. */
    fun isVerified(validCheckIns: Int, distinctUsers: Int): Boolean =
        validCheckIns >= rules.verifiedMinCheckIns && distinctUsers >= rules.verifiedMinUsers
}
