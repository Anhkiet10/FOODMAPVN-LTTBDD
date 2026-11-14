package com.example.foodmapvn.data.model


// Trạng thái đề xuất quán
enum class SuggestionStatus { PENDING, APPROVED, REJECTED }

// Đề xuất quán gửi, hiển thị ở màn hình "Đề xuất của tôi" và  Admin. //
data class PlaceSuggestion(
    val id: String,
    val userId: String,
    val regionId: String,
    val dishId: String,
    val name: String,
    val address: String,
    val location: LatLng,
    val price: Long?,
    val openHours: String?,
    val photoUrl: String?,
    val status: SuggestionStatus,
    val rejectReason: String?,
    val createdAt: String?,
)

// Dữ liệu người dùng nhập vào khi gửi đề xuất new
data class NewSuggestion(
    val regionId: String,
    val dishId: String,
    val name: String,
    val address: String,
    val location: LatLng,
    val price: Long?,
    val openHours: String?,
    val photoUrl: String?,
)

// ===== Thông báo (UC-16) – Nhật, Tuần 2 =====
/** Loại thông báo – khớp danh sách cho phép trong firestore.rules > isValidNotification. */
enum class NotificationType {
    SUGGESTION_APPROVED, SUGGESTION_REJECTED, BADGE, HELPFUL, PRICE_WARNING;

    companion object {
        fun from(code: String?): NotificationType = entries.firstOrNull { it.name == code } ?: BADGE
    }
}

/** Một thông báo của người dùng: users/{uid}/notifications/{id} (UC-16). */
data class NotificationItem(
    val id: String,
    val type: NotificationType,
    val title: String,
    val body: String?,
    /** Mã tham chiếu: placeId (đề xuất được duyệt), suggestionId (bị từ chối)... */
    val refId: String?,
    val read: Boolean,
    /** "dd/MM/yyyy HH:mm"; null = đang chờ server ghi. */
    val createdAt: String?,
)

// ===== Quản trị (UC-17..UC-20) – Nhật, Tuần 3-4 =====
// ===== Mô hình dữ liệu cho khu vực Quản trị (Người 3 – Nhật) =====

/** Số liệu màn "Admin tổng quan" (UC-20). */
data class AdminStats(
    val pendingSuggestions: Int = 0,
    val newReports: Int = 0,
    val warnedPlaces: Int = 0,
    val reviewsThisWeek: Int = 0,
    val regionCount: Int = 0,
    val dishCount: Int = 0,
    val userCount: Int = 0,
)

/** Một đề xuất quán kèm tên món, vùng, người đề xuất để hiển thị (UC-17). */
data class AdminSuggestion(
    val suggestion: PlaceSuggestion,
    val dishName: String,
    val regionName: String,
    val proposerName: String,
)

/** Trạng thái báo cáo – khớp reports.status trong firestore.rules. */
enum class AdminReportStatus(val code: String) {
    PENDING("pending"), RESOLVED("resolved"), DISMISSED("dismissed");

    companion object {
        fun from(value: String?) = entries.firstOrNull { it.code == value } ?: PENDING
    }
}

/** Một báo cáo vi phạm (UC-18). [reasonCode] = WRONG_PRICE / BAD_CONTENT / NOT_EXIST / OTHER. */
data class AdminReport(
    val id: String,
    val targetType: String,   // "PLACE" | "REVIEW"
    val targetId: String,
    val title: String,        // tên quán (hoặc quán của đánh giá)
    val quote: String?,       // nội dung đánh giá / mô tả của người báo cáo
    val reasonCode: String,
    val reporterName: String,
    val status: AdminReportStatus,
    val createdAt: String?,
) {
    val reasonLabel: String
        get() = when (reasonCode) {
            "WRONG_PRICE" -> "Thông tin sai"
            "BAD_CONTENT" -> "Nội dung không phù hợp"
            "NOT_EXIST" -> "Quán đóng cửa"
            else -> "Đánh giá giả"
        }
}

data class AdminRegion(val id: String, val name: String, val dishCount: Int, val placeCount: Int)

data class AdminDish(
    val id: String,
    val name: String,
    val priceText: String,
    val placeCount: Int,
    val visible: Boolean,
)

data class AdminUser(
    val uid: String,
    val name: String,
    val email: String,
    val isAdmin: Boolean,
    val locked: Boolean,
)