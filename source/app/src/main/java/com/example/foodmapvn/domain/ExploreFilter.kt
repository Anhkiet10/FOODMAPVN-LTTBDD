package com.example.foodmapvn.domain

import com.example.foodmapvn.data.model.Dish
import com.example.foodmapvn.data.model.LatLng
import com.example.foodmapvn.data.model.PlaceSummary

/** Bộ lọc nâng cao ở Khám phá (UC-05). null / false = không lọc theo tiêu chí đó. */
data class ExploreFilters(
    /** Giá tối đa một phần (đồng). */
    val maxPrice: Long? = null,
    /** Khoảng cách tối đa (km) — chỉ có tác dụng khi biết vị trí. */
    val maxDistanceKm: Double? = null,
    /** Điểm uy tín tối thiểu (0–100). */
    val minTrust: Int? = null,
    val openNow: Boolean = false,
    val hidePriceWarning: Boolean = false,
) {
    val activeCount: Int
        get() = listOf(maxPrice != null, maxDistanceKm != null, minTrust != null, openNow, hidePriceWarning).count { it }

    val isEmpty: Boolean get() = activeCount == 0
}

/** Món sau khi lọc: kèm khoảng cách tới quán phù hợp gần nhất (null = chưa biết vị trí). */
data class DishMatch(val dish: Dish, val nearestMeters: Double?, val matchingPlaceCount: Int)

object OpeningHours {
    /** "06:30" -> 390 (phút trong ngày); sai định dạng -> null. */
    fun toMinutes(hhmm: String?): Int? {
        val parts = hhmm?.split(":") ?: return null
        val h = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return if (h in 0..24 && m in 0..59) h * 60 + m else null
    }

    /** Đang mở cửa không; null = không rõ giờ. Hỗ trợ quán mở qua nửa đêm (vd. 18:00–02:00). */
    fun isOpen(openTime: String?, closeTime: String?, nowMinutes: Int): Boolean? {
        val open = toMinutes(openTime) ?: return null
        val close = toMinutes(closeTime) ?: return null
        return if (open <= close) nowMinutes in open until close
        else nowMinutes >= open || nowMinutes < close
    }
}

object ExploreFilter {

    fun distanceMeters(place: PlaceSummary, me: LatLng?): Double? = me?.let {
        GeoUtils.distanceMeters(it.latitude, it.longitude, place.location.latitude, place.location.longitude)
    }

    /** Quán có thỏa bộ lọc không (khi xét món [dishId], giá lấy theo menu của quán cho món đó). */
    fun placePasses(
        place: PlaceSummary,
        filters: ExploreFilters,
        me: LatLng?,
        nowMinutes: Int,
        dishId: String? = null,
        fallbackPrice: Long? = null,
    ): Boolean {
        if (filters.hidePriceWarning && place.priceWarning) return false
        filters.minTrust?.let { if (place.trustScore < it) return false }
        if (filters.openNow && OpeningHours.isOpen(place.openTime, place.closeTime, nowMinutes) != true) return false
        filters.maxDistanceKm?.let { maxKm ->
            val d = distanceMeters(place, me)
            if (d != null && d > maxKm * 1000) return false // chưa biết vị trí -> bỏ qua tiêu chí khoảng cách
        }
        filters.maxPrice?.let { maxPrice ->
            val price = (dishId?.let { place.minPriceByDish[it] } ?: place.minPriceByDish.values.minOrNull()) ?: fallbackPrice
            if (price == null || price > maxPrice) return false
        }
        return true
    }

    /**
     * Lọc món: giữ món có ít nhất một quán thỏa bộ lọc; "gần nhất" = quán thỏa bộ lọc gần vị trí thật nhất.
     * Không có bộ lọc: giữ mọi món (kể cả món chưa có quán), sắp xếp như cũ.
     */
    fun filterDishes(
        dishes: List<Dish>,
        places: List<PlaceSummary>,
        filters: ExploreFilters,
        me: LatLng?,
        nowMinutes: Int,
    ): List<DishMatch> = dishes.mapNotNull { dish ->
        val serving = places.filter { dish.id in it.dishIds }
        val passing = serving.filter { placePasses(it, filters, me, nowMinutes, dish.id, dish.priceLow) }
        if (!filters.isEmpty && passing.isEmpty()) return@mapNotNull null
        val nearest = passing.mapNotNull { distanceMeters(it, me) }.minOrNull()
        DishMatch(dish, nearest, passing.size)
    }

    /** Lọc quán (kết quả Tìm kiếm), gần trước nếu biết vị trí, không thì uy tín cao trước. */
    fun sortPlaces(places: List<PlaceSummary>, me: LatLng?): List<PlaceSummary> =
        if (me == null) places.sortedByDescending { it.trustScore }
        else places.sortedBy { distanceMeters(it, me) }
}
