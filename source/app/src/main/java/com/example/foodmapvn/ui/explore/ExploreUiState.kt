package com.example.foodmapvn.ui.explore

import com.example.foodmapvn.data.model.Dish
import com.example.foodmapvn.domain.ExploreFilters

data class ExploreUiState(
    val regionName: String = "",
    val dishCount: Int = 0,
    val placeCount: Int = 0,
    val isOpenNow: Boolean = false,
    val filters: List<String> = emptyList(),
    val selectedFilter: String = ALL,
    /** Bộ lọc nâng cao (UC-05). */
    val advancedFilters: ExploreFilters = ExploreFilters(),
    /** Đã có vị trí thật: "gần nhất" và lọc khoảng cách dùng được. */
    val hasLocation: Boolean = false,
    val dishes: List<Dish> = emptyList(),
) {
    val subtitle: String
        get() = buildList {
            add("$dishCount đặc sản")
            add("$placeCount quán")
            if (isOpenNow) add("Đang mở cửa")
        }.joinToString(" • ")

    companion object {
        const val ALL = "Tất cả"
    }
}
