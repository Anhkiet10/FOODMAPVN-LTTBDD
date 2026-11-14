package com.example.foodmapvn.data.firebase.mapper

import com.example.foodmapvn.data.model.AdminDish
import com.example.foodmapvn.data.model.AdminRegion
import com.example.foodmapvn.data.model.AdminUser
import com.example.foodmapvn.data.model.LatLng
import com.example.foodmapvn.data.model.NotificationItem
import com.example.foodmapvn.data.model.NotificationType
import com.example.foodmapvn.data.model.PlaceSuggestion
import com.example.foodmapvn.data.model.SuggestionStatus
import com.google.firebase.firestore.DocumentSnapshot
import java.text.SimpleDateFormat
import java.util.Locale


// null nếu document thiếu vị trí (dữ liệu hỏng) -> danh sách tự bỏ qua
fun DocumentSnapshot.toPlaceSuggestion(): PlaceSuggestion? {
    val point = geo("location") ?: return null
    return PlaceSuggestion(
        id = id,
        userId = str("userId") ?: "",
        regionId = str("regionId") ?: "",
        dishId = str("dishId") ?: "",
        name = str("name") ?: "",
        address = str("address") ?: "",
        location = LatLng(point.latitude, point.longitude),
        price = long("price"),
        openHours = str("openHours"),
        photoUrl = str("photoUrl"),
        status = when (str("status")) {
            "approved" -> SuggestionStatus.APPROVED
            "rejected" -> SuggestionStatus.REJECTED
            else -> SuggestionStatus.PENDING
        },
        rejectReason = str("rejectReason"),
        createdAt = time("createdAt")?.toDate()
            ?.let { SimpleDateFormat("dd/MM/yyyy", Locale.US).format(it) } ?: "",
    )
}

// ----- Thông báo: users/{uid}/notifications/{id} -----
fun DocumentSnapshot.toNotificationItem(): NotificationItem = NotificationItem(
    id = id,
    type = NotificationType.from(str("type")),
    title = str("title") ?: "",
    body = str("body"),
    refId = str("refId"),
    read = bool("read") ?: false,
    createdAt = time("createdAt")?.toDate()
        ?.let { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(it) },
)

// ----- Admin: vùng, món, người dùng -----
fun DocumentSnapshot.toAdminRegion(): AdminRegion =
    AdminRegion(id, str("name") ?: id, int("dishCount"), int("placeCount"))

fun DocumentSnapshot.toAdminDish(): AdminDish {
    val lo = int("priceP25") / 1000
    val hi = int("priceP75") / 1000
    return AdminDish(
        id = id, name = str("name") ?: id,
        priceText = if (hi > 0) "$lo – ${hi}k" else "—",
        placeCount = int("verifiedPlaceCount"),
        visible = str("status") != "hidden",
    )
}

fun DocumentSnapshot.toAdminUser(): AdminUser = AdminUser(
    uid = id, name = str("name") ?: "—",
    email = str("email") ?: str("phone") ?: "",
    isAdmin = str("role") == "admin",
    locked = str("status") == "locked",
)