package com.example.foodmapvn.data.model

/** Một quán tôi đã check-in (màn "Huy hiệu của tôi"). */
data class CheckedInPlace(
    val placeId: String,
    val name: String,
    /** Số lần check-in hợp lệ tại quán. */
    val count: Int,
    /** Lần check-in gần nhất (epoch millis). */
    val lastAtMillis: Long,
)

/** Check-in của tôi trong một vùng (tỉnh/thành) – dùng để tính huy hiệu Người địa phương (BR-09). */
data class RegionCheckIns(
    val regionId: String,
    val checkIns: Int,
    val places: List<CheckedInPlace>,
    /** Thời điểm đạt huy hiệu (lần check-in làm đủ điều kiện); null = chưa đạt. */
    val achievedAtMillis: Long?,
)
