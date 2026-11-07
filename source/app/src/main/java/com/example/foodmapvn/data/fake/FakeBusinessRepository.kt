package com.example.foodmapvn.data.fake

import com.example.foodmapvn.data.model.Business
import com.example.foodmapvn.data.model.BusinessStatus
import com.example.foodmapvn.data.model.BusinessType
import com.example.foodmapvn.data.repository.BusinessRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Bản giả (chưa có Firebase): giữ trong bộ nhớ. Không có Admin thật nên đăng ký xong
 * được tự duyệt sau khi gửi (để thử luồng đề xuất quán khi làm giao diện).
 */
class FakeBusinessRepository : BusinessRepository {
    private val mine = MutableStateFlow<Business?>(null)

    override fun getMyBusiness(): Flow<Business?> = mine
    override fun canSuggestPlace(): Flow<Boolean> = mine.map { it?.canSuggestPlace == true }

    override suspend fun register(name: String, type: BusinessType): Result<Unit> {
        BusinessRepository.validateName(name)?.let { return Result.failure(IllegalArgumentException(it)) }
        mine.value = Business(ownerId = "fake-user", name = name.trim(), type = type, status = BusinessStatus.APPROVED)
        return Result.success(Unit)
    }

    override fun getPendingBusinesses(): Flow<List<Business>> =
        mine.map { listOfNotNull(it?.takeIf { b -> b.status == BusinessStatus.PENDING }) }

    override suspend fun review(ownerId: String, approve: Boolean, rejectReason: String?): Result<Unit> {
        mine.update { it?.copy(status = if (approve) BusinessStatus.APPROVED else BusinessStatus.REJECTED, rejectReason = rejectReason) }
        return Result.success(Unit)
    }
}
