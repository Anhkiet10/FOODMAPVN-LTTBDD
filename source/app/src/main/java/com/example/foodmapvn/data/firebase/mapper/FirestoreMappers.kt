package com.example.foodmapvn.data.firebase.mapper

import com.example.foodmapvn.data.model.Dish
import com.example.foodmapvn.data.model.FoodArtType
import com.example.foodmapvn.data.model.LatLng
import com.example.foodmapvn.data.model.PlacePin
import com.example.foodmapvn.data.model.RegionBadge
import com.example.foodmapvn.data.model.RegionSummary
import com.example.foodmapvn.data.model.ReviewSummary
import com.example.foodmapvn.data.model.SavedDish
import com.example.foodmapvn.data.model.SavedPlace
import com.example.foodmapvn.data.model.UserProfile
import com.example.foodmapvn.domain.BadgeCalculator
import com.example.foodmapvn.domain.formatVnd
import com.google.firebase.firestore.DocumentSnapshot
import java.text.SimpleDateFormat
import java.util.Locale

// Chuyển document Firestore -> model của app. Tên trường xem mục 8.2 tài liệu đặc tả.
// Mọi trường đọc qua các hàm an toàn trong SafeFields.kt: thiếu hoặc sai kiểu -> giá trị mặc định, không crash.

private fun DocumentSnapshot.art(): FoodArtType =
    str("art")?.let { runCatching { FoodArtType.valueOf(it) }.getOrNull() } ?: FoodArtType.BUN_BO

private fun DocumentSnapshot.dateText(field: String, pattern: String): String? =
    time(field)?.toDate()?.let { SimpleDateFormat(pattern, Locale.US).format(it) }

fun DocumentSnapshot.toRegionSummary() = RegionSummary(
    name = str("name") ?: "",
    dishCount = int("dishCount"),
    placeCount = int("placeCount"),
    isOpenNow = bool("isOpenNow") ?: true,
    quickFilters = strings("quickFilters"),
)

fun DocumentSnapshot.toDish(): Dish {
    val low = long("priceP25")
    val high = long("priceP75")
    val hasRange = low != null && high != null
    return Dish(
        id = id,
        name = str("name") ?: "",
        verifiedPlaceCount = int("verifiedPlaceCount"),
        nearestDistance = str("nearestHint") ?: "—",
        marketPrice = if (hasRange) "${low!! / 1000}–${high!! / 1000}k" else null,
        bestTime = str("bestTime"),
        priceRange = if (hasRange) "${formatVnd(low!!)}–${formatVnd(high!!)}đ" else "Chưa có giá",
        priceUnit = str("unit") ?: "phần",
        art = art(),
        photoCount = strings("photos").size.coerceAtLeast(1),
        imageUrl = str("imageUrl"),
        priceLow = low,
    )
}

fun DocumentSnapshot.toPlacePin(): PlacePin? {
    val point = geo("location") ?: return null
    return PlacePin(
        id = id,
        name = str("name") ?: "",
        location = LatLng(point.latitude, point.longitude),
        trustScore = int("trustScore"),
        hasPriceWarning = bool("priceWarning") ?: false,
    )
}

fun DocumentSnapshot.toSavedDish() = SavedDish(
    id = id,
    name = str("dishName") ?: "",
    placeName = str("placeName"),
    distance = str("distanceHint"),
    isEaten = bool("eaten") ?: false,
    art = art(),
    imageUrl = str("imageUrl"),
)

fun DocumentSnapshot.toSavedPlace() = SavedPlace(
    id = id,
    name = str("name") ?: "",
    dishName = str("dishName") ?: "",
    distance = str("distanceHint") ?: "—",
    trustScore = int("trustScore"),
    art = art(),
    imageUrl = str("imageUrl"),
)

fun DocumentSnapshot.toUserProfile() = UserProfile(
    name = str("name") ?: "Người dùng",
    memberSince = dateText("createdAt", "MM/yyyy") ?: "",
    localBadge = str("localBadge"),
    checkInCount = int("stats.checkIns"),
    reviewCount = int("stats.reviews"),
    suggestedPlaceCount = int("stats.suggestions"),
    avatarUrl = str("avatarUrl"),
)

fun DocumentSnapshot.toRegionBadge(calculator: BadgeCalculator): RegionBadge {
    val progress = calculator.progress(
        checkIns = int("checkIns"),
        distinctPlaces = int("placeCount"),
        achievedDate = dateText("achievedAt", "dd/MM/yyyy"),
    )
    return RegionBadge(
        region = str("regionName") ?: id,
        status = progress.status,
        description = progress.description,
        progress = progress.progress,
    )
}

fun DocumentSnapshot.toReviewSummary() = ReviewSummary(
    id = id,
    placeName = str("placeName") ?: "",
    rating = int("stars"),
    isCheckedIn = bool("hasCheckIn") ?: false,
    price = long("price")?.let { "${formatVnd(it)}đ" } ?: "",
)
