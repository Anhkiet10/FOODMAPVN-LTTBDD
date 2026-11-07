package com.example.foodmapvn.data.repository

import com.example.foodmapvn.data.model.DishDetail
import com.example.foodmapvn.data.model.PlaceDetail
import com.example.foodmapvn.data.model.ReviewItem
import com.example.foodmapvn.domain.CheckInResult
import kotlinx.coroutines.flow.Flow

/**
 * Quản lý dữ liệu chi tiết quán, chi tiết đặc sản, check-in và đánh giá (Người 2).
 */
interface PlaceRepository {

    /** Lấy chi tiết quán theo placeId. */
    fun getPlaceDetail(placeId: String): Flow<PlaceDetail?>

    /** Lấy chi tiết đặc sản theo dishId. */
    fun getDishDetail(dishId: String): Flow<DishDetail?>

    /** Lấy danh sách đánh giá của quán. */
    fun getPlaceReviews(placeId: String): Flow<List<ReviewItem>>

    /** Đã lưu quán này chưa. */
    fun isPlaceSaved(placeId: String): Flow<Boolean>

    /** Lưu hoặc bỏ lưu quán. */
    suspend fun toggleSavePlace(placeId: String)

    /** Lưu hoặc bỏ lưu món muốn ăn. */
    suspend fun toggleSaveDish(dishId: String)

    /** Thực hiện check-in vị trí tại quán (UC-12). */
    suspend fun performCheckIn(
        placeId: String,
        userLat: Double,
        userLon: Double,
    ): Result<CheckInResult>

    /** Gửi đánh giá cho quán (UC-13). */
    suspend fun submitReview(
        placeId: String,
        rating: Int,
        content: String,
        price: Long?,
        photoUrls: List<String>,
    ): Result<Unit>
}
