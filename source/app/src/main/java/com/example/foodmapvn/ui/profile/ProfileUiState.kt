package com.example.foodmapvn.ui.profile

import com.example.foodmapvn.data.model.BusinessStatus
import com.example.foodmapvn.data.model.RegionBadge
import com.example.foodmapvn.data.model.ReviewSummary
import com.example.foodmapvn.data.model.UserProfile

data class ProfileUiState(
    val user: UserProfile,
    val hasUnreadNotification: Boolean = false,
    val regionBadges: List<RegionBadge> = emptyList(),
    val recentReviews: List<ReviewSummary> = emptyList(),
    val wishlistCount: Int = 0,
    val mySuggestionCount: Int = 0,
    /** Tài khoản admin -> hiện dòng "Quản trị". */
    val isAdmin: Boolean = false,
    /** Trạng thái đăng ký doanh nghiệp; null = chưa đăng ký. Chỉ APPROVED mới đề xuất quán được. */
    val businessStatus: BusinessStatus? = null,
)
