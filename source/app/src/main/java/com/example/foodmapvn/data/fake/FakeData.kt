package com.example.foodmapvn.data.fake

import com.example.foodmapvn.data.model.BadgeStatus
import com.example.foodmapvn.data.model.Dish
import com.example.foodmapvn.data.model.FoodArtType
import com.example.foodmapvn.data.model.LatLng
import com.example.foodmapvn.data.model.PlacePin
import com.example.foodmapvn.data.model.RegionBadge
import com.example.foodmapvn.data.model.RegionSummary
import com.example.foodmapvn.data.model.ReviewSummary
import com.example.foodmapvn.data.model.RouteSuggestion
import com.example.foodmapvn.data.model.SavedDish
import com.example.foodmapvn.data.model.SavedPlace
import com.example.foodmapvn.data.model.UserProfile

/**
 * Dữ liệu giả, chỉ được dùng bởi các Fake…Repository (và @Preview).
 */
object FakeData {

    val regionSummary = RegionSummary(
        name = "Huế",
        dishCount = 7,
        placeCount = 12,
        isOpenNow = true,
        quickFilters = listOf("Bún bò", "Cơm hến", "Chè"),
    )

    val dishes = listOf(
        Dish(
            id = "bun-bo-hue",
            name = "Bún bò Huế",
            verifiedPlaceCount = 12,
            nearestDistance = "350 m",
            marketPrice = "35–50k",
            bestTime = "Ngon nhất buổi sáng",
            priceRange = "35.000–50.000đ",
            priceUnit = "tô",
            art = FoodArtType.BUN_BO,
        ),
        Dish(
            id = "che-hue",
            name = "Chè Huế",
            verifiedPlaceCount = 18,
            nearestDistance = "200 m",
            priceRange = "10.000–20.000đ",
            priceUnit = "cốc",
            art = FoodArtType.CHE,
        ),
        Dish(
            id = "com-hen",
            name = "Cơm hến",
            verifiedPlaceCount = 9,
            nearestDistance = "1,1 km",
            marketPrice = "15–25k",
            bestTime = "Ngon nhất buổi trưa",
            priceRange = "15.000–25.000đ",
            priceUnit = "đĩa",
            art = FoodArtType.COM_HEN,
        ),
        Dish(
            id = "banh-canh-nam-pho",
            name = "Bánh canh Nam Phổ",
            verifiedPlaceCount = 6,
            nearestDistance = "800 m",
            marketPrice = "20–30k",
            priceRange = "20.000–30.000đ",
            priceUnit = "tô",
            art = FoodArtType.BUN_BO,
        ),
    )

    val savedDishes = listOf(
        SavedDish("s1", "Bún bò Huế", "O Hạnh", "350 m", isEaten = true, art = FoodArtType.BUN_BO),
        SavedDish("s2", "Cơm hến", "Bà Nở", "1,1 km", isEaten = true, art = FoodArtType.COM_HEN),
        SavedDish("s3", "Chè Huế", "Chè Hẻm", "600 m", isEaten = true, art = FoodArtType.CHE),
        SavedDish("s4", "Bánh canh Nam Phổ", null, null, isEaten = false, art = FoodArtType.BUN_BO),
        SavedDish("s5", "Bánh khoái", "Lạc Thiện", "1,4 km", isEaten = false, art = FoodArtType.COM_HEN),
    )

    val savedPlaces = listOf(
        SavedPlace("p1", "Bún bò O Hạnh", "Bún bò Huế", "350 m", trustScore = 89, art = FoodArtType.BUN_BO),
        SavedPlace("p2", "Chè Hẻm", "Chè Huế", "600 m", trustScore = 86, art = FoodArtType.CHE),
        SavedPlace("p3", "Cơm hến Bà Nở", "Cơm hến", "1,1 km", trustScore = 84, art = FoodArtType.COM_HEN),
    )

    /** Toạ độ quanh trung tâm Huế, chỉ để thử bản đồ. */
    val placePins = listOf(
        PlacePin("p1", "Bún bò O Hạnh", LatLng(16.4655, 107.5905), trustScore = 89, hasPriceWarning = false),
        PlacePin("p2", "Chè Hẻm", LatLng(16.4690, 107.5960), trustScore = 86, hasPriceWarning = false),
        PlacePin("p3", "Cơm hến Bà Nở", LatLng(16.4720, 107.5850), trustScore = 84, hasPriceWarning = false),
        PlacePin("p4", "Bánh canh Nam Phổ", LatLng(16.4610, 107.5980), trustScore = 80, hasPriceWarning = false),
        PlacePin("p5", "Hoàng Cung", LatLng(16.4680, 107.5880), trustScore = 58, hasPriceWarning = true),
    )

    val routeSuggestion = RouteSuggestion(
        placeCount = 2,
        totalDistance = "2,1 km",
        note = "đi ít đường nhất",
    )

    val user = UserProfile(
        name = "Nguyễn Văn Khoa",
        memberSince = "09/2026",
        localBadge = "Người địa phương · Huế",
        checkInCount = 23,
        reviewCount = 14,
        suggestedPlaceCount = 2,
    )

    val regionBadges = listOf(
        RegionBadge("Huế", BadgeStatus.ACHIEVED, "Đã đạt · từ 12/09/2026", progress = 1f),
        RegionBadge("Đắk Lắk", BadgeStatus.IN_PROGRESS, "1 / 5 lần check-in", progress = 0.2f),
        RegionBadge("Cần Thơ", BadgeStatus.LOCKED, "Chưa có check-in", progress = 0f),
    )

    val recentReviews = listOf(
        ReviewSummary("r1", "Bún bò O Hạnh", rating = 5, isCheckedIn = true, price = "45.000đ"),
        ReviewSummary("r2", "Chè Hẻm Huế", rating = 4, isCheckedIn = true, price = "15.000đ"),
    )
}
