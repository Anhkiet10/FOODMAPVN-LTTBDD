package com.example.foodmapvn.data.firebase.mapper

import com.example.foodmapvn.data.model.LatLng
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