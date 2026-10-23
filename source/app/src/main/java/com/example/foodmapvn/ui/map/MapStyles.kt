package com.example.foodmapvn.ui.map

/**
 * Kiểu bản đồ vector của OpenFreeMap (dữ liệu OpenStreetMap) — miễn phí, không cần đăng ký hay API key.
 * Xem trước: https://openfreemap.org
 * Muốn đổi kiểu bản đồ của cả app, chỉ cần sửa [DEFAULT].
 */
object MapStyles {
    /** Giống Google Maps: nhiều màu, dễ nhìn. */
    const val LIBERTY = "https://tiles.openfreemap.org/styles/liberty"

    /** Tươi sáng, đường phố rõ. */
    const val BRIGHT = "https://tiles.openfreemap.org/styles/bright"

    /** Nền xám nhạt, tối giản — hợp khi cần làm nổi ghim quán. */
    const val POSITRON = "https://tiles.openfreemap.org/styles/positron"

    const val DEFAULT = LIBERTY
}
