package com.example.foodmapvn.domain

import com.example.foodmapvn.data.model.LatLng
import com.example.foodmapvn.data.model.Province
import java.text.Normalizer

/**
 * Suy ra tỉnh/thành từ vị trí:
 * 1. Ưu tiên tên tỉnh mà Geocoder trả về (vd. "Thành phố Hồ Chí Minh", "Bình Dương")
 *    — so cả tên mới lẫn tên tỉnh cũ đã sáp nhập.
 * 2. Không có tên / không khớp → lấy tỉnh có trung tâm gần nhất.
 */
object ProvinceLocator {

    fun locate(location: LatLng, adminAreaName: String?, provinces: List<Province>): Province? =
        matchByName(adminAreaName, provinces) ?: nearest(location, provinces)

    fun matchByName(name: String?, provinces: List<Province>): Province? {
        val key = name?.let(::normalize)?.takeIf { it.isNotBlank() } ?: return null
        return provinces.firstOrNull { p -> (listOf(p.name) + p.formerNames).any { normalize(it) == key } }
    }

    fun nearest(location: LatLng, provinces: List<Province>): Province? =
        provinces.minByOrNull {
            GeoUtils.distanceMeters(location.latitude, location.longitude, it.center.latitude, it.center.longitude)
        }

    /** "Thành phố Hồ Chí Minh" / "TP. Hồ Chí Minh" / "Ho Chi Minh City" -> "ho chi minh". */
    fun normalize(text: String): String {
        var s = Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace('đ', 'd').replace('Đ', 'D')
            .lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        for (prefix in listOf("thanh pho ", "tinh ", "tp ")) if (s.startsWith(prefix)) s = s.removePrefix(prefix)
        for (suffix in listOf(" city", " province")) if (s.endsWith(suffix)) s = s.removeSuffix(suffix)
        return when (s) {
            "sai gon" -> "ho chi minh"
            "thua thien hue" -> "hue"
            else -> s
        }
    }
}
