package com.example.foodmapvn.data.fake

import com.example.foodmapvn.data.model.RegionBadge
import com.example.foodmapvn.data.model.ReviewSummary
import com.example.foodmapvn.data.model.UserProfile
import com.example.foodmapvn.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeUserRepository : UserRepository {
    override fun getUser(): Flow<UserProfile> = flowOf(FakeData.user)
    override fun getRegionBadges(): Flow<List<RegionBadge>> = flowOf(FakeData.regionBadges)
    override fun getRecentReviews(): Flow<List<ReviewSummary>> = flowOf(FakeData.recentReviews)
    override fun hasUnreadNotification(): Flow<Boolean> = flowOf(true)
}
