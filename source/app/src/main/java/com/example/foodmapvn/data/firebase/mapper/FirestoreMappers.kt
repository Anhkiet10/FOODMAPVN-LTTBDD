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

// Chuyển document Firestore -> model của app. Tên trường xem mục 8 tài liệu đặc tả.

private fun DocumentSnapshot.int(field: String): Int = getLong(field)?.toInt() ?: 0

private fun DocumentSnapshot.art(): FoodArtType =
    getString("art")?.let { runCatching { FoodArtType.valueOf(it) }.getOrNull() } ?: FoodArtType.BUN_BO

private fun DocumentSnapshot.strings(field: String): List<String> =
    (get(field) as? List<*>)?.filterIsInstance<String>() ?: emptyList()

private fun DocumentSnapshot.dateText(field: String, pattern: String): String? =
    getTimestamp(field)?.toDate()?.let { SimpleDateFormat(pattern, Locale.US).format(it) }

fun DocumentSnapshot.toRegionSummary() = RegionSummary(
    name = getString("name") ?: "",
    dishCount = int("dishCount"),
    placeCount = int("placeCount"),
    isOpenNow = getBoolean("isOpenNow") ?: true,
    quickFilters = strings("quickFilters"),
)

fun DocumentSnapshot.toDish(): Dish {
    val low = getLong("priceP25")
    val high = getLong("priceP75")
    val hasRange = low != null && high != null
    return Dish(
        id = id,
        name = getString("name") ?: "",
        verifiedPlaceCount = int("verifiedPlaceCount"),
        nearestDistance = getString("nearestHint") ?: "—",
        marketPrice = if (hasRange) "${low!! / 1000}–${high!! / 1000}k" else null,
        bestTime = getString("bestTime"),
        priceRange = if (hasRange) "${formatVnd(low!!)}–${formatVnd(high!!)}đ" else "Chưa có giá",
        priceUnit = getString("unit") ?: "phần",
        art = art(),
        photoCount = strings("photos").size.coerceAtLeast(1),
        imageUrl = getString("imageUrl"),
    )
}

fun DocumentSnapshot.toPlacePin(): PlacePin? {
    val point = getGeoPoint("location") ?: return null
    return PlacePin(
        id = id,
        name = getString("name") ?: "",
        location = LatLng(point.latitude, point.longitude),
        trustScore = int("trustScore"),
        hasPriceWarning = getBoolean("priceWarning") ?: false,
    )
}

fun DocumentSnapshot.toSavedDish() = SavedDish(
    id = id,
    name = getString("dishName") ?: "",
    placeName = getString("placeName"),
    distance = getString("distanceHint"),
    isEaten = getBoolean("eaten") ?: false,
    art = art(),
    imageUrl = getString("imageUrl"),
)

fun DocumentSnapshot.toSavedPlace() = SavedPlace(
    id = id,
    name = getString("name") ?: "",
    dishName = getString("dishName") ?: "",
    distance = getString("distanceHint") ?: "—",
    trustScore = int("trustScore"),
    art = art(),
    imageUrl = getString("imageUrl"),
)

fun DocumentSnapshot.toUserProfile() = UserProfile(
    name = getString("name") ?: "Người dùng",
    memberSince = dateText("createdAt", "MM/yyyy") ?: "",
    localBadge = getString("localBadge"),
    checkInCount = int("stats.checkIns"),
    reviewCount = int("stats.reviews"),
    suggestedPlaceCount = int("stats.suggestions"),
)

fun DocumentSnapshot.toRegionBadge(calculator: BadgeCalculator): RegionBadge {
    val progress = calculator.progress(
        checkIns = int("checkIns"),
        distinctPlaces = int("placeCount"),
        achievedDate = dateText("achievedAt", "dd/MM/yyyy"),
    )
    return RegionBadge(
        region = getString("regionName") ?: id,
        status = progress.status,
        description = progress.description,
        progress = progress.progress,
    )
}

fun DocumentSnapshot.toReviewSummary() = ReviewSummary(
    id = id,
    placeName = getString("placeName") ?: "",
    rating = int("stars"),
    isCheckedIn = getBoolean("hasCheckIn") ?: false,
    price = getLong("price")?.let { "${formatVnd(it)}đ" } ?: "",
)
