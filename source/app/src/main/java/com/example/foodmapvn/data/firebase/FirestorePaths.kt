package com.example.foodmapvn.data.firebase

/** Tên các collection trên Firestore (xem mục 8 tài liệu đặc tả). */
object FirestorePaths {
    const val REGIONS = "regions"
    const val DISHES = "dishes"
    const val PLACES = "places"
    const val REVIEWS = "reviews"          // id = "${placeId}_${userId}" → 1 đánh giá / người / quán
    const val CHECKINS = "checkins"
    const val USERS = "users"
    const val WISHLIST = "wishlist"        // users/{uid}/wishlist/{dishId}
    const val SAVED_PLACES = "savedPlaces" // users/{uid}/savedPlaces/{placeId}
    const val BADGES = "badges"            // users/{uid}/badges/{regionId}
    const val NOTIFICATIONS = "notifications"
    const val SUGGESTIONS = "suggestions"
    const val REPORTS = "reports"
    const val CONFIG = "config"
}
