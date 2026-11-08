package com.example.foodmapvn.data.model

/**
 * Chi tiết một món đặc sản (UC-08) — Người 2.
 * Đọc từ dishes/{dishId} (mục 8.2 tài liệu đặc tả).
 */
data class DishDetail(
    val id: String,
    val regionId: String,
    val name: String,
    val description: String,
    /** Mẹo ăn, vd. "Ăn buổi sáng 6–9h, nước dùng ngon nhất". */
    val tips: List<String>,
    /** vd. "Ngon nhất buổi sáng"; null = không có. */
    val bestTime: String?,
    /** Mặt bằng giá, vd. "35.000–50.000đ" hoặc "Chưa có giá". */
    val priceRange: String,
    /** tô / cốc / đĩa / phần. */
    val priceUnit: String,
    val category: FoodCategory,
    val art: FoodArtType,
    val imageUrl: String?,
    val verifiedPlaceCount: Int,
)

/** Cách sắp xếp danh sách "Quán ngon gần bạn". */
enum class PlaceSort(val label: String) {
    TRUST("Uy tín cao"),
    DISTANCE("Gần nhất"),
    /** Figma: giá thấp nhất trước (quán chưa có giá xếp sau). */
    PRICE("Giá tốt"),
    /** Figma: quán đang mở trước. */
    OPEN_NOW("Đang mở"),
}

/** Một quán bán món, đã tính sẵn cho màn Chi tiết đặc sản. */
data class DishPlace(
    val place: PlaceSummary,
    /** Khoảng cách tới vị trí của tôi (mét); null = chưa có vị trí. */
    val distanceMeters: Double?,
    /** Giá thấp nhất của món này trong menu quán (đồng); null = chưa có giá. */
    val minPrice: Long?,
    /** true/false = đang mở/đã đóng; null = không rõ giờ. */
    val isOpen: Boolean?,
)
