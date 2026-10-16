package com.example.foodmapvn.domain

import com.example.foodmapvn.data.model.BadgeStatus

data class BadgeProgress(val status: BadgeStatus, val progress: Float, val description: String)

/** BR-09: huy hiệu Người địa phương theo vùng. */
class BadgeCalculator(private val rules: BusinessRules = BusinessRules()) {

    fun progress(checkIns: Int, distinctPlaces: Int, achievedDate: String? = null): BadgeProgress {
        val achieved = checkIns >= rules.localBadgeCheckIns && distinctPlaces >= rules.localBadgeMinPlaces
        return when {
            achieved -> BadgeProgress(
                BadgeStatus.ACHIEVED, 1f,
                if (achievedDate != null) "Đã đạt · từ $achievedDate" else "Đã đạt",
            )
            checkIns == 0 -> BadgeProgress(BadgeStatus.LOCKED, 0f, "Chưa có check-in")
            else -> BadgeProgress(
                BadgeStatus.IN_PROGRESS,
                (checkIns.toFloat() / rules.localBadgeCheckIns).coerceAtMost(0.99f),
                "$checkIns / ${rules.localBadgeCheckIns} lần check-in",
            )
        }
    }
}
