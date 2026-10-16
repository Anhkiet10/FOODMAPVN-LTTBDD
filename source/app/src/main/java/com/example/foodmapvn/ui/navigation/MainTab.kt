package com.example.foodmapvn.ui.navigation

import androidx.annotation.DrawableRes
import com.example.foodmapvn.R

/** Các tab ở thanh điều hướng dưới. */
enum class MainTab(val label: String, @DrawableRes val icon: Int) {
    EXPLORE("Khám phá", R.drawable.ic_home),
    MAP("Bản đồ", R.drawable.ic_place),
    SAVED("Đã lưu", R.drawable.ic_bookmark),
    PROFILE("Hồ sơ", R.drawable.ic_person),
}
