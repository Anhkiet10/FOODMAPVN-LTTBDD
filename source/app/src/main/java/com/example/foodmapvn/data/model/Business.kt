package com.example.foodmapvn.data.model

/** Loại hình kinh doanh (businesses.type). */
enum class BusinessType(val value: String, val label: String) {
    HOUSEHOLD("household", "Hộ kinh doanh"),
    COMPANY("company", "Công ty / Doanh nghiệp");

    companion object {
        fun from(value: String?): BusinessType = entries.firstOrNull { it.value == value } ?: HOUSEHOLD
    }
}

/** Trạng thái duyệt (businesses.status). Chỉ APPROVED mới được đề xuất quán (UC-14). */
enum class BusinessStatus(val value: String, val label: String) {
    PENDING("pending", "Đang chờ duyệt"),
    APPROVED("approved", "Đã xác minh"),
    REJECTED("rejected", "Bị từ chối");

    companion object {
        fun from(value: String?): BusinessStatus = entries.firstOrNull { it.value == value } ?: PENDING
    }
}

/**
 * Hồ sơ doanh nghiệp của một tài khoản — businesses/{uid} (mỗi tài khoản tối đa 1).
 * Chủ quán đăng ký -> Admin duyệt -> được đề xuất quán lên bản đồ.
 */
data class Business(
    val ownerId: String,
    val name: String,
    val type: BusinessType,
    val status: BusinessStatus,
    /** Lý do từ chối (khi status = REJECTED). */
    val rejectReason: String? = null,
    /** "dd/MM/yyyy" ngày gửi đăng ký; null = chưa có. */
    val createdAt: String? = null,
) {
    val canSuggestPlace: Boolean get() = status == BusinessStatus.APPROVED
}
