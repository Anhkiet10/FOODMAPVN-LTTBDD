package com.example.foodmapvn.data.repository

import com.example.foodmapvn.data.model.NotificationItem
import kotlinx.coroutines.flow.Flow

/** Thông báo của người dùng đang đăng nhập (UC-16): xem danh sách, đánh dấu đã đọc. */
interface NotificationRepository {
    /** Mới nhất trước; Khách (chưa đăng nhập) = danh sách rỗng. */
    fun getNotifications(): Flow<List<NotificationItem>>

    suspend fun markRead(id: String): Result<Unit>

    suspend fun markAllRead(): Result<Unit>
}