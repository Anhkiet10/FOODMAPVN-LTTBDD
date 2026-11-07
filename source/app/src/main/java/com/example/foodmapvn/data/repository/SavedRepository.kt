package com.example.foodmapvn.data.repository

import com.example.foodmapvn.data.model.DishDetail
import com.example.foodmapvn.data.model.RouteSuggestion
import com.example.foodmapvn.data.model.SavedDish
import com.example.foodmapvn.data.model.SavedPlace
import kotlinx.coroutines.flow.Flow

/** Món muốn ăn và quán đã lưu của người dùng. */
interface SavedRepository {
    fun getSavedDishes(): Flow<List<SavedDish>>
    fun getSavedPlaces(): Flow<List<SavedPlace>>
    fun getRouteSuggestion(): Flow<RouteSuggestion?>
    suspend fun toggleEaten(dishId: String)

    // --- UC-10: lưu món từ màn Chi tiết đặc sản (Người 2) ---
    /** Món đã có trong "Món muốn ăn" chưa (Khách luôn false). */
    fun isDishSaved(dishId: String): Flow<Boolean>

    /**
     * Thêm món vào users/{uid}/wishlist/{dishId}.
     * [placeId]/[placeName]/[distanceHint]: quán gợi ý (quán đứng đầu danh sách), có thể null.
     */
    suspend fun saveDish(dish: DishDetail, placeId: String?, placeName: String?, distanceHint: String?): Result<Unit>

    suspend fun removeDish(dishId: String): Result<Unit>
}
