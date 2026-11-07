package com.example.foodmapvn.data.firebase.mapper

import com.example.foodmapvn.data.model.DishDetail
import com.example.foodmapvn.data.model.FoodArtType
import com.example.foodmapvn.data.model.FoodCategory
import com.example.foodmapvn.domain.formatVnd
import com.google.firebase.firestore.DocumentSnapshot

/** dishes/{dishId} -> DishDetail (Người 2). Thiếu trường thì dùng giá trị mặc định, không crash. */
fun DocumentSnapshot.toDishDetail(): DishDetail {
    val low = long("priceP25")
    val high = long("priceP75")
    return DishDetail(
        id = id,
        regionId = str("regionId") ?: "",
        name = str("name") ?: "",
        description = str("description") ?: "",
        tips = strings("tips"),
        bestTime = str("bestTime"),
        priceRange = if (low != null && high != null) "${formatVnd(low)}–${formatVnd(high)}đ" else "Chưa có giá",
        priceUnit = str("unit") ?: "phần",
        category = FoodCategory.from(str("category")),
        art = str("art")?.let { runCatching { FoodArtType.valueOf(it) }.getOrNull() } ?: FoodArtType.BUN_BO,
        imageUrl = str("imageUrl"),
        verifiedPlaceCount = int("verifiedPlaceCount"),
    )
}
