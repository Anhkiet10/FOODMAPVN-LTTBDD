package com.example.foodmapvn.data.fake

import com.example.foodmapvn.data.model.RegionBadge
import com.example.foodmapvn.data.model.ReviewSummary
import com.example.foodmapvn.data.model.UserProfile
import com.example.foodmapvn.data.repository.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update

class FakeUserRepository : UserRepository {
    private val user = MutableStateFlow(FakeData.user)

    override fun getUser(): Flow<UserProfile> = user
    override fun getRegionBadges(): Flow<List<RegionBadge>> = flowOf(FakeData.regionBadges)
    override fun getRecentReviews(): Flow<List<ReviewSummary>> = flowOf(FakeData.recentReviews)
    override fun hasUnreadNotification(): Flow<Boolean> = flowOf(true)

    override suspend fun updateProfile(name: String, avatarUrl: String?): Result<Unit> {
        delay(400)
        user.update { it.copy(name = name.trim(), avatarUrl = avatarUrl ?: it.avatarUrl) }
        return Result.success(Unit)
    }
}
