package com.example.foodmapvn.data.model

/**
 * Quán ở dạng rút gọn cho Tìm kiếm và Bộ lọc nâng cao (Người 1).
 * Chi tiết đầy đủ của quán là việc của Người 2 (PlaceModels.kt).
 */
data class PlaceSummary(
    val id: String,
    val name: String,
    val address: String,
    val location: LatLng,
    /** "HH:mm"; null = chưa có giờ mở cửa. */
    val openTime: String?,
    val closeTime: String?,
    val trustScore: Int,
    val isVerified: Boolean,
    val priceWarning: Boolean,
    val ratingAvg: Double,
    /** Các món quán có bán (dishes/{dishId}). */
    val dishIds: List<String>,
    /** Giá thấp nhất trong menu theo từng món (đồng). */
    val minPriceByDish: Map<String, Long> = emptyMap(),
)
