package com.example.foodmapvn.data.repository

import com.example.foodmapvn.data.model.AdminDish
import com.example.foodmapvn.data.model.AdminRegion
import com.example.foodmapvn.data.model.AdminReport
import com.example.foodmapvn.data.model.AdminReportStatus
import com.example.foodmapvn.data.model.AdminStats
import com.example.foodmapvn.data.model.AdminSuggestion
import com.example.foodmapvn.data.model.AdminUser
import com.example.foodmapvn.data.model.SuggestionStatus
import kotlinx.coroutines.flow.Flow

/**
 *  (Người 3 – Nhật): Tổng quan, Duyệt quán, Báo cáo, Đặc sản & vùng, Người dùng.
 *  các thao tác ghi đều cần users/{uid}.role == "admin" (xem firebase/firestore.rules).
 */
interface AdminRepository {
    // UC-20 Tổng quan
    fun getStats(): Flow<AdminStats>

    // UC-17 Duyệt quán đề xuất
    fun getSuggestions(status: SuggestionStatus): Flow<List<AdminSuggestion>>
    /** Duyệt: tạo quán (places), đổi đề xuất sang approved, gửi thông báo cho người đề xuất. */
    suspend fun approveSuggestion(item: AdminSuggestion): Result<Unit>
    /** Từ chối (bắt buộc lý do), gửi thông báo cho người đề xuất. */
    suspend fun rejectSuggestion(item: AdminSuggestion, reason: String): Result<Unit>

    // UC-18 Báo cáo vi phạm
    fun getReports(status: AdminReportStatus): Flow<List<AdminReport>>
    suspend fun dismissReport(report: AdminReport): Result<Unit>
    /** Ẩn đánh giá bị báo cáo rồi đóng báo cáo. */
    suspend fun hideReportedReview(report: AdminReport): Result<Unit>
    /** Đánh dấu quán đóng cửa rồi đóng báo cáo. */
    suspend fun markPlaceClosed(report: AdminReport): Result<Unit>
    /** Đóng báo cáo là "đã xử lý" (Admin đã sửa thông tin quán). */
    suspend fun resolveReport(report: AdminReport): Result<Unit>

    // UC-19 Đặc sản & vùng
    fun getRegions(): Flow<List<AdminRegion>>
    fun getDishes(regionId: String): Flow<List<AdminDish>>
    suspend fun setDishVisible(dishId: String, visible: Boolean): Result<Unit>
    suspend fun addDish(regionId: String, name: String, unit: String): Result<Unit>

    // Người dùng
    fun getUsers(): Flow<List<AdminUser>>
    suspend fun setUserLocked(uid: String, locked: Boolean): Result<Unit>
}