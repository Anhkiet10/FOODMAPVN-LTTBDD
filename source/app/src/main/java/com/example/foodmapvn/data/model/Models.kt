package com.example.foodmapvn.data.model

/** Ảnh minh hoạ tạm, vẽ bằng Canvas. Khi có ảnh thật thì thay bằng imageUrl. */
enum class FoodArtType { BUN_BO, CHE, COM_HEN }

/** Một món đặc sản hiển thị ở màn Khám phá. */
data class Dish(
    val id: String,
    val name: String,
    val verifiedPlaceCount: Int,
    val nearestDistance: String,
    val marketPrice: String? = null,
    val bestTime: String? = null,
    val priceRange: String,
    val priceUnit: String,
    val art: FoodArtType,
    val photoCount: Int = 5,
    val imageUrl: String? = null,
)

/** Món trong danh sách "Món muốn ăn". */
data class SavedDish(
    val id: String,
    val name: String,
    val placeName: String?,
    val distance: String?,
    val isEaten: Boolean,
    val art: FoodArtType,
    val imageUrl: String? = null,
)

/** Quán người dùng đã lưu. */
data class SavedPlace(
    val id: String,
    val name: String,
    val dishName: String,
    val distance: String,
    val trustScore: Int,
    val art: FoodArtType,
    val imageUrl: String? = null,
)

data class RouteSuggestion(
    val placeCount: Int,
    val totalDistance: String,
    val note: String,
)

data class UserProfile(
    val name: String,
    val memberSince: String,
    val localBadge: String?,
    val checkInCount: Int,
    val reviewCount: Int,
    val suggestedPlaceCount: Int,
) {
    /** Chữ cái đại diện: lấy chữ đầu của tên (từ cuối cùng). */
    val avatarInitial: String
        get() = name.trim().split(" ").lastOrNull()?.firstOrNull()?.uppercase() ?: "?"
}

enum class BadgeStatus { ACHIEVED, IN_PROGRESS, LOCKED }

data class RegionBadge(
    val region: String,
    val status: BadgeStatus,
    val description: String,
    val progress: Float,
)

data class ReviewSummary(
    val id: String,
    val placeName: String,
    val rating: Int,
    val isCheckedIn: Boolean,
    val price: String,
)

/** Thông tin tổng quan của vùng đang xem (ô tìm kiếm ở màn Khám phá). */
data class RegionSummary(
    val name: String,
    val dishCount: Int,
    val placeCount: Int,
    val isOpenNow: Boolean,
    val quickFilters: List<String>,
) {
    companion object {
        /** Tỉnh chưa có dữ liệu đặc sản. */
        fun empty(name: String) = RegionSummary(name, 0, 0, isOpenNow = false, quickFilters = emptyList())
    }
}

/** Người dùng đã đăng nhập (từ Firebase Auth). */
data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
)

data class LatLng(val latitude: Double, val longitude: Double)

/** Ghim quán trên bản đồ. */
data class PlacePin(
    val id: String,
    val name: String,
    val location: LatLng,
    val trustScore: Int,
    val hasPriceWarning: Boolean,
)
