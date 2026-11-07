package com.example.foodmapvn.data.repository

import com.example.foodmapvn.data.model.Business
import com.example.foodmapvn.data.model.BusinessType
import kotlinx.coroutines.flow.Flow

/**
 * Đăng ký doanh nghiệp (chủ quán) — chỉ doanh nghiệp đã được Admin duyệt mới được đề xuất quán.
 *
 * Người 1: phần người dùng (đăng ký, xem trạng thái) + [canSuggestPlace].
 * Người 3: dùng [canSuggestPlace] ở màn Đề xuất quán; dùng [getPendingBusinesses] / [review]
 *          để làm tab "Duyệt doanh nghiệp" trong màn Admin.
 */
interface BusinessRepository {
    /** Doanh nghiệp của tài khoản đang đăng nhập; null = chưa đăng ký (hoặc là Khách). */
    fun getMyBusiness(): Flow<Business?>

    /** true khi doanh nghiệp đã được duyệt -> được tạo đề xuất quán. */
    fun canSuggestPlace(): Flow<Boolean>

    /**
     * Gửi đăng ký (lần đầu) hoặc gửi lại sau khi bị từ chối. Trạng thái về "Đang chờ duyệt".
     * Lỗi nếu chưa đăng nhập, tên không hợp lệ, hoặc doanh nghiệp đã được duyệt.
     */
    suspend fun register(name: String, type: BusinessType): Result<Unit>

    // ----- Dành cho Admin (Người 3) -----
    /** Các đăng ký đang chờ duyệt, cũ nhất trước. */
    fun getPendingBusinesses(): Flow<List<Business>>

    /** Duyệt ([approve] = true) hoặc từ chối (bắt buộc [rejectReason]). */
    suspend fun review(ownerId: String, approve: Boolean, rejectReason: String? = null): Result<Unit>

    companion object {
        const val NAME_MIN = 2
        const val NAME_MAX = 100

        /** null = hợp lệ; ngược lại là thông báo lỗi tiếng Việt. */
        fun validateName(name: String): String? {
            val n = name.trim()
            return when {
                n.length < NAME_MIN -> "Tên doanh nghiệp cần ít nhất $NAME_MIN ký tự"
                n.length > NAME_MAX -> "Tên doanh nghiệp tối đa $NAME_MAX ký tự"
                n.none { it.isLetter() } -> "Tên doanh nghiệp cần có chữ cái"
                else -> null
            }
        }
    }
}
