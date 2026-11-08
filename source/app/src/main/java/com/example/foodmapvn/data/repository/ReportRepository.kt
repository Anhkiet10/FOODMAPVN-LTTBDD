package com.example.foodmapvn.data.repository

/**
 * Lý do báo cáo (UC-15). [code] khớp danh sách cho phép trong firestore.rules > isValidReport.
 * Nhãn hiển thị theo Figma "Báo cáo vi phạm".
 */
enum class ReportReason(val code: String, val label: String) {
    FAKE("OTHER", "Đánh giá giả / spam"),
    CLOSED("NOT_EXIST", "Quán đã đóng cửa"),
    WRONG_INFO("WRONG_PRICE", "Thông tin sai (giá, giờ mở, địa chỉ)"),
    BAD_CONTENT("BAD_CONTENT", "Nội dung không phù hợp"),
}

/** Gửi báo cáo vi phạm cho Admin xử lý (reports/{id}). */
interface ReportRepository {
    /** @param targetType "REVIEW" hoặc "PLACE". */
    suspend fun report(targetType: String, targetId: String, reason: ReportReason, note: String): Result<Unit>
}
