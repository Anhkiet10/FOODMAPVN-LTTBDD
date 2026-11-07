package com.example.foodmapvn.ui.components

import androidx.annotation.DrawableRes
import com.example.foodmapvn.R
import com.example.foodmapvn.data.model.FoodCategory

/**
 * Icon của từng loại món (res/drawable/ic_cat_*.xml).
 * 8 icon lấy từ Material Icons của Google (Apache License 2.0, ghi nguồn trong báo cáo);
 * "Bánh truyền thống" tự vẽ vì Material không có món tương ứng.
 */
@get:DrawableRes
val FoodCategory.iconRes: Int
    get() = when (this) {
        FoodCategory.NOODLE_SOUP -> R.drawable.ic_cat_noodle_soup
        FoodCategory.RICE -> R.drawable.ic_cat_rice
        FoodCategory.BANH_MI -> R.drawable.ic_cat_banh_mi
        FoodCategory.TRADITIONAL_CAKE -> R.drawable.ic_cat_traditional_cake
        FoodCategory.HOTPOT_GRILL -> R.drawable.ic_cat_hotpot_grill
        FoodCategory.SEAFOOD -> R.drawable.ic_cat_seafood
        FoodCategory.DESSERT -> R.drawable.ic_cat_dessert
        FoodCategory.DRINK -> R.drawable.ic_cat_drink
        FoodCategory.OTHER -> R.drawable.ic_cat_other
    }
