package com.example.foodmapvn.data.model


// Trạng thái đề xuất quán
enum class SuggestionStatus { PENDING, APPROVED, REJECTED }

// Đề xuất quán gửi, hiển thị ở màn hình "Đề xuất của tôi" và  Admin. //
data class PlaceSuggestion(
    val id: String,
    val userId: String,
    val regionId: String,
    val dishId: String,
    val name: String,
    val address: String,
    val location: LatLng,
    val price: Long?,
    val openHours: String?,
    val photoUrl: String?,
    val status: SuggestionStatus,
    val rejectReason: String?,
    val createdAt: String?,
)

// Dữ liệu người dùng nhập vào khi gửi đề xuất new
data class NewSuggestion(
    val regionId: String,
    val dishId: String,
    val name: String,
    val address: String,
    val location: LatLng,
    val price: Long?,
    val openHours: String?,
    val photoUrl: String?,
)