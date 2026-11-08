package com.example.foodmapvn.data.repository

import com.example.foodmapvn.data.model.RegionBadge
import com.example.foodmapvn.data.model.RegionCheckIns
import com.example.foodmapvn.data.model.ReviewSummary
import com.example.foodmapvn.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Hồ sơ người dùng: thông tin cá nhân, huy hiệu, đánh giá. */
interface UserRepository {
    fun getUser(): Flow<UserProfile>
    fun getRegionBadges(): Flow<List<RegionBadge>>
    fun getRecentReviews(): Flow<List<ReviewSummary>>
    /** Tất cả đánh giá của tôi, mới nhất trước (màn "Đánh giá của tôi"). */
    fun getAllReviews(): Flow<List<ReviewSummary>>
    fun hasUnreadNotification(): Flow<Boolean>

    /**
     * Check-in hợp lệ của tôi, gom theo vùng (khóa = id tỉnh) – màn "Huy hiệu của tôi" (34 vùng).
     * Tính trực tiếp từ collection checkins nên luôn khớp với lần check-in thật.
     */
    fun getCheckInsByRegion(): Flow<Map<String, RegionCheckIns>> = flowOf(emptyMap())

    /**
     * Sửa hồ sơ (UC-16): đổi tên hiển thị và/hoặc ảnh đại diện.
     * [avatarUrl] = null -> giữ ảnh cũ. Ảnh phải tải lên Cloudinary trước (ImageUploader) rồi truyền URL vào đây.
     */
    suspend fun updateProfile(name: String, avatarUrl: String?): Result<Unit>
}
