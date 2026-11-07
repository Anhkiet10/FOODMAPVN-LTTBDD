package com.example.foodmapvn.data.model

/**
 * Loại món (8 + 1) — quyết định icon của ghim quán trên bản đồ.
 * - dishes.category: loại của món.
 * - places.category: icon của quán; Admin chọn khi duyệt / thêm quán lên bản đồ
 *   (dữ liệu mẫu: lấy theo món chính = món đầu tiên trong dishIds).
 * [value] là giá trị lưu trong Firestore (mục 8.2 tài liệu đặc tả).
 */
enum class FoodCategory(val value: String, val label: String) {
    NOODLE_SOUP("noodle_soup", "Món nước"),           // bún, phở, hủ tiếu, bánh canh, mì, cháo
    RICE("rice", "Cơm"),                               // cơm tấm, cơm hến, cơm gà, xôi
    BANH_MI("banh_mi", "Bánh mì"),
    TRADITIONAL_CAKE("traditional_cake", "Bánh truyền thống"), // bèo, nậm, lọc, xèo, khoái, cuốn, căn
    HOTPOT_GRILL("hotpot_grill", "Lẩu & nướng"),       // lẩu, nướng, xiên, nem lụi
    SEAFOOD("seafood", "Hải sản"),                     // ốc, cua, tôm, cá
    DESSERT("dessert", "Chè & tráng miệng"),           // chè, kem, bánh ngọt
    DRINK("drink", "Cà phê & đồ uống"),
    OTHER("other", "Khác");

    companion object {
        /** Giá trị lạ / thiếu -> OTHER (không crash khi dữ liệu sai). */
        fun from(value: String?): FoodCategory = entries.firstOrNull { it.value == value } ?: OTHER
    }
}
