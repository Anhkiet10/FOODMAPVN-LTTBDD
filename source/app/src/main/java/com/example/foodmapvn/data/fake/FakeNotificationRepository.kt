package com.example.foodmapvn.data.fake

import com.example.foodmapvn.data.model.NotificationItem
import com.example.foodmapvn.data.model.NotificationType
import com.example.foodmapvn.data.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Thông báo mẫu khi chưa có google-services.json. */
class FakeNotificationRepository : NotificationRepository {

    private val items = MutableStateFlow(
        listOf(
            NotificationItem("n1", NotificationType.SUGGESTION_APPROVED, "Đề xuất quán đã được duyệt",
                "Quán \"Chè Cung Đình\" đã được thêm lên bản đồ.", "fake-place-1", false, "13/11/2026 09:30"),
            NotificationItem("n2", NotificationType.SUGGESTION_REJECTED, "Đề xuất quán bị từ chối",
                "Quán \"Cơm hến Hẻm 5\": thông tin địa chỉ chưa chính xác.", "fake-3", false, "12/11/2026 16:05"),
            NotificationItem("n3", NotificationType.HELPFUL, "Đánh giá của bạn hữu ích",
                "3 người thấy đánh giá của bạn về Bún bò Huế là hữu ích.", null, true, "10/11/2026 20:12"),
            NotificationItem("n4", NotificationType.BADGE, "Bạn nhận huy hiệu mới",
                "Chúc mừng! Bạn đã mở khóa huy hiệu Huế.", null, true, "08/11/2026 11:00"),
        )
    )

    override fun getNotifications(): Flow<List<NotificationItem>> = items

    override suspend fun markRead(id: String): Result<Unit> {
        items.update { list -> list.map { if (it.id == id) it.copy(read = true) else it } }
        return Result.success(Unit)
    }

    override suspend fun markAllRead(): Result<Unit> {
        items.update { list -> list.map { it.copy(read = true) } }
        return Result.success(Unit)
    }
}