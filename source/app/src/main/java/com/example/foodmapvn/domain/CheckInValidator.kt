package com.example.foodmapvn.domain

sealed interface CheckInResult {
    data class Valid(val distanceMeters: Double) : CheckInResult
    data class TooFar(val distanceMeters: Double, val maxMeters: Double) : CheckInResult
    data class TooSoon(val nextAllowedAtMillis: Long) : CheckInResult
}

/** BR-01, BR-02: kiểm tra một lần check-in có hợp lệ không. */
class CheckInValidator(private val rules: BusinessRules = BusinessRules()) {

    fun validate(
        userLat: Double,
        userLon: Double,
        placeLat: Double,
        placeLon: Double,
        lastCheckInAtMillis: Long?,
        nowMillis: Long = System.currentTimeMillis(),
    ): CheckInResult {
        if (lastCheckInAtMillis != null) {
            val nextAllowed = lastCheckInAtMillis + rules.checkInCooldownHours * 3_600_000L
            if (nowMillis < nextAllowed) return CheckInResult.TooSoon(nextAllowed)
        }
        val distance = GeoUtils.distanceMeters(userLat, userLon, placeLat, placeLon)
        return if (distance <= rules.checkInRadiusMeters) CheckInResult.Valid(distance)
        else CheckInResult.TooFar(distance, rules.checkInRadiusMeters)
    }

    /** BR-03: đánh giá có được gắn nhãn "Check-in" không. */
    fun reviewHasCheckIn(lastCheckInAtMillis: Long?, reviewAtMillis: Long): Boolean {
        if (lastCheckInAtMillis == null) return false
        val window = rules.reviewCheckInWindowDays * 86_400_000L
        return reviewAtMillis - lastCheckInAtMillis in 0..window
    }
}
