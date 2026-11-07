package com.example.foodmapvn.data.repository

import com.example.foodmapvn.data.model.DishDetail
import com.example.foodmapvn.data.model.PlaceSummary
import kotlinx.coroutines.flow.Flow

/** Dữ liệu cho màn Chi tiết đặc sản (UC-08) — Người 2. */
interface DishDetailRepository {
    /** Chi tiết món; null = không tìm thấy hoặc món đã bị ẩn. */
    fun getDish(dishId: String): Flow<DishDetail?>

    /** Các quán đang hoạt động có bán món này (places.dishIds chứa dishId). */
    fun getPlacesServing(dishId: String): Flow<List<PlaceSummary>>
}
