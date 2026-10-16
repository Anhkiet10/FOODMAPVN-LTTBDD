package com.example.foodmapvn.data.repository

import com.example.foodmapvn.data.model.LatLng

interface LocationRepository {
    fun hasPermission(): Boolean
    /** Vị trí hiện tại; null nếu chưa có quyền hoặc không lấy được. */
    suspend fun currentLocation(): LatLng?
}
