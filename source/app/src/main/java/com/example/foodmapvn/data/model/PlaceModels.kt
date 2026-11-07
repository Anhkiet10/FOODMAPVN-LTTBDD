package com.example.foodmapvn.data.model

/** Thông tin chi tiết của quán (màn Chi tiết quán - UC-09). */
data class PlaceDetail(
    val id: String,
    val name: String,
    val address: String,
    val location: LatLng,
    val openTime: String?,
    val closeTime: String?,
    val trustScore: Int,
    val isVerified: Boolean,
    val hasPriceWarning: Boolean,
    val ratingAvg: Double,
    val reviewCount: Int,
    val checkInCount: Int,
    val imageUrl: String? = null,
    val photos: List<String> = emptyList(),
    val dishIds: List<String> = emptyList(),
    val dishes: List<Dish> = emptyList(),
    val minPriceByDish: Map<String, Long> = emptyMap(),
    val isSaved: Boolean = false,
    val regionId: String = "",
)

/** Chi tiết của một đặc sản (màn Chi tiết đặc sản). */
data class DishDetail(
    val dish: Dish,
    val description: String? = null,
    val bestTime: String? = null,
    val nearestHint: String? = null,
    val tips: List<String> = emptyList(),
    val photos: List<String> = emptyList(),
    val nearbyPlaces: List<PlaceSummary> = emptyList(),
    val isSaved: Boolean = false,
)

/** Đánh giá của quán (UC-11, UC-13). */
data class ReviewItem(
    val id: String,
    val placeId: String,
    val placeName: String,
    val userId: String,
    val userName: String,
    val userAvatar: String? = null,
    val rating: Int,
    val content: String?,
    val photos: List<String> = emptyList(),
    val createdAt: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val isCheckedIn: Boolean = false,
    val isLocal: Boolean = false,
    val price: Long? = null,
    val helpfulCount: Int = 0,
)

/** Bản ghi check-in tại quán (UC-12). */
data class CheckInRecord(
    val id: String,
    val userId: String,
    val placeId: String,
    val regionId: String,
    val location: LatLng,
    val distanceMeters: Double,
    val isValid: Boolean,
    val createdAtMillis: Long = System.currentTimeMillis(),
)
