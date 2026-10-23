package com.example.foodmapvn.data.repository

import com.example.foodmapvn.data.model.Province
import kotlinx.coroutines.flow.StateFlow

/**
 * Khu vực (tỉnh/thành) đang chọn — DÙNG CHUNG cho cả app.
 * Khám phá, Bản đồ, Tìm kiếm… đều đọc từ đây; chọn ở đâu thì mọi màn cùng đổi theo.
 * Lựa chọn được lưu lại, mở lại app vẫn giữ.
 */
interface AreaRepository {
    /** null = chưa chọn lần nào (lần đầu mở app) → Splash chuyển sang màn Chọn vùng. */
    val selectedArea: StateFlow<Province?>

    fun select(province: Province)
}
