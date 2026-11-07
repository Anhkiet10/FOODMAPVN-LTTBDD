package com.example.foodmapvn.domain

import com.example.foodmapvn.data.model.DishPlace
import com.example.foodmapvn.data.model.LatLng
import com.example.foodmapvn.data.model.PlaceSort
import com.example.foodmapvn.data.model.PlaceSummary

/**
 * "Quán ngon gần bạn" ở màn Chi tiết đặc sản (UC-08) — Người 2.
 *
 * - Uy tín cao: quán đã có đánh giá trước (điểm uy tín giảm dần, quán xác minh trước),
 *   quán chưa có đánh giá xếp sau; cùng hạng thì quán gần hơn đứng trước.
 * - Gần nhất: theo khoảng cách tăng dần; chưa có vị trí thì dùng thứ tự uy tín.
 */
object DishPlaceSorter {

    fun sort(
        places: List<PlaceSummary>,
        dishId: String,
        me: LatLng?,
        nowMinutes: Int,
        sort: PlaceSort,
    ): List<DishPlace> {
        val items = places.map { p ->
            DishPlace(
                place = p,
                distanceMeters = ExploreFilter.distanceMeters(p, me),
                minPrice = p.minPriceByDish[dishId],
                isOpen = OpeningHours.isOpen(p.openTime, p.closeTime, nowMinutes),
            )
        }
        val byTrust = compareByDescending<DishPlace> { it.place.hasReviews }
            .thenByDescending { if (it.place.hasReviews) it.place.trustScore else 0 }
            .thenByDescending { it.place.isVerified }
            .thenBy { it.distanceMeters ?: Double.MAX_VALUE }
            .thenBy { it.place.name }
        return when {
            sort == PlaceSort.DISTANCE && me != null ->
                items.sortedWith(compareBy<DishPlace> { it.distanceMeters ?: Double.MAX_VALUE }.then(byTrust))
            else -> items.sortedWith(byTrust)
        }
    }
}
