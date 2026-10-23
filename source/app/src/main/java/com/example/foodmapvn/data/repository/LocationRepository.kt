package com.example.foodmapvn.data.repository

import com.example.foodmapvn.data.model.LatLng

interface LocationRepository {
    fun hasPermission(): Boolean
    /** Vị trí hiện tại; null nếu chưa có quyền hoặc không lấy được. */
    suspend fun currentLocation(): LatLng?
    /** Tên khu vực (tỉnh/thành phố) của một vị trí, vd. "Thành phố Hồ Chí Minh"; null nếu không tra được. */
    suspend fun areaName(location: LatLng): String?
}
