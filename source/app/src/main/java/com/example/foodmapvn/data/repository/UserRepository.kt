package com.example.foodmapvn.data.repository

import com.example.foodmapvn.data.model.RegionBadge
import com.example.foodmapvn.data.model.ReviewSummary
import com.example.foodmapvn.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

/** Hồ sơ người dùng: thông tin cá nhân, huy hiệu, đánh giá. */
interface UserRepository {
    fun getUser(): Flow<UserProfile>
    fun getRegionBadges(): Flow<List<RegionBadge>>
    fun getRecentReviews(): Flow<List<ReviewSummary>>
    fun hasUnreadNotification(): Flow<Boolean>
}
