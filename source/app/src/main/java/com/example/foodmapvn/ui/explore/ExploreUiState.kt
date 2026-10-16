package com.example.foodmapvn.ui.explore

import com.example.foodmapvn.data.model.Dish

data class ExploreUiState(
    val regionName: String = "",
    val dishCount: Int = 0,
    val placeCount: Int = 0,
    val isOpenNow: Boolean = false,
    val filters: List<String> = emptyList(),
    val selectedFilter: String = ALL,
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
