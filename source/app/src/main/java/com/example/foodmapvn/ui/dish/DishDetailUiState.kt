package com.example.foodmapvn.ui.dish

import com.example.foodmapvn.data.model.DishDetail
import com.example.foodmapvn.data.model.DishPlace
import com.example.foodmapvn.data.model.PlaceSort

/** Trạng thái màn Chi tiết đặc sản (UC-08, UC-10) — Người 2. */
data class DishDetailUiState(
    val isLoading: Boolean = true,
    /** Không có món này (sai ID hoặc món đã bị ẩn). */
    val notFound: Boolean = false,
    val dish: DishDetail? = null,
    /** Quán bán món, đã sắp xếp theo [sort]. */
    val places: List<DishPlace> = emptyList(),
    val sort: PlaceSort = PlaceSort.TRUST,
    /** Đã có vị trí thật -> hiện khoảng cách, sắp "Gần nhất" được. */
    val hasLocation: Boolean = false,
    /** Món đã có trong "Món muốn ăn". */
    val isSaved: Boolean = false,
    val isSaving: Boolean = false,
    /** Thông báo ngắn (Snackbar), vd. "Đã lưu vào Món muốn ăn". */
    val message: String? = null,
)
