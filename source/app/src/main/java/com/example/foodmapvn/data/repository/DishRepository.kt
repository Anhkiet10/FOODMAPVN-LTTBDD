package com.example.foodmapvn.data.repository

import com.example.foodmapvn.data.model.Dish
import com.example.foodmapvn.data.model.PlacePin
import com.example.foodmapvn.data.model.PlaceSummary
import com.example.foodmapvn.data.model.RegionSummary
import kotlinx.coroutines.flow.Flow

/** Dữ liệu của một vùng: thông tin vùng, các món đặc sản, các quán (bản đồ). */
interface DishRepository {
    fun getRegionSummary(): Flow<RegionSummary>
    fun getDishes(): Flow<List<Dish>>
    fun getPlacePins(): Flow<List<PlacePin>>

    /** Quán đang hoạt động của khu vực đang chọn, đủ thông tin cho tìm kiếm và bộ lọc (UC-05, UC-06). */
    fun getPlaces(): Flow<List<PlaceSummary>>
}
