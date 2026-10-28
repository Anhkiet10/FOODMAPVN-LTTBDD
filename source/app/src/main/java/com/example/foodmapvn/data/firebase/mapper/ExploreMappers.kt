package com.example.foodmapvn.data.firebase.mapper

import com.example.foodmapvn.data.model.LatLng
import com.example.foodmapvn.data.model.PlaceSummary
import com.google.firebase.firestore.DocumentSnapshot

// Mapper của Người 1 cho Tìm kiếm / Bộ lọc. Đọc qua hàm an toàn (SafeFields.kt).

/** places/{placeId} -> PlaceSummary; thiếu toạ độ thì bỏ qua quán. */
fun DocumentSnapshot.toPlaceSummary(): PlaceSummary? {
    val point = geo("location") ?: return null
    // menu: [{dishId, name, price}] -> giá thấp nhất theo từng món
    val minPrice = mutableMapOf<String, Long>()
    (get("menu") as? List<*>)?.forEach { item ->
        val map = item as? Map<*, *> ?: return@forEach
        val dishId = map["dishId"] as? String ?: return@forEach
        val price = (map["price"] as? Number)?.toLong() ?: return@forEach
        minPrice[dishId] = minOf(price, minPrice[dishId] ?: Long.MAX_VALUE)
    }
    return PlaceSummary(
        id = id,
        name = str("name") ?: "",
        address = str("address") ?: "",
        location = LatLng(point.latitude, point.longitude),
        openTime = str("openTime"),
        closeTime = str("closeTime"),
        trustScore = int("trustScore"),
        isVerified = bool("isVerified") ?: false,
        priceWarning = bool("priceWarning") ?: false,
        ratingAvg = double("ratingAvg") ?: 0.0,
        dishIds = strings("dishIds"),
        minPriceByDish = minPrice,
    )
}
