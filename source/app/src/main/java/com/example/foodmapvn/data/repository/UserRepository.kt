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

    /**
     * Sửa hồ sơ (UC-16): đổi tên hiển thị và/hoặc ảnh đại diện.
     * [avatarUrl] = null -> giữ ảnh cũ. Ảnh phải tải lên Cloudinary trước (ImageUploader) rồi truyền URL vào đây.
     */
    suspend fun updateProfile(name: String, avatarUrl: String?): Result<Unit>
}
