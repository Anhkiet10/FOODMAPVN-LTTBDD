package com.example.foodmapvn.data.firebase

import com.example.foodmapvn.data.firebase.mapper.toNotificationItem
import com.example.foodmapvn.data.model.NotificationItem
import com.example.foodmapvn.data.repository.NotificationRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * users/{uid}/notifications/{id}: người dùng đọc/sửa thông báo của chính mình
 * (firestore.rules > match /users/{uid}/notifications). Đọc trường theo kiểu an toàn: sai kiểu thì bỏ qua.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreNotificationRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : NotificationRepository {

    private fun col(uid: String) =
        db.collection(FirestorePaths.USERS).document(uid).collection(FirestorePaths.NOTIFICATIONS)

    private fun DocumentSnapshot.stamp(): Timestamp? = get("createdAt") as? Timestamp

    override fun getNotifications(): Flow<List<NotificationItem>> = auth.uidFlow().flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList())
        else col(uid).asFlow().map { snap ->
            snap.documents
                // createdAt chưa có (đang chờ server ghi) -> coi là mới nhất
                .sortedByDescending { it.stamp()?.seconds ?: Long.MAX_VALUE }
                .map { it.toNotificationItem() }
        }
    }

    private suspend fun attempt(block: suspend () -> Unit): Result<Unit> = try {
        block(); Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val msg = when {
            e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Không có quyền cập nhật thông báo (kiểm tra Rules đã Publish)"
            e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.UNAVAILABLE ->
                "Không có kết nối mạng"
            else -> e.message ?: "Đã có lỗi xảy ra"
        }
        Result.failure(Exception(msg, e))
    }

    override suspend fun markRead(id: String): Result<Unit> = attempt {
        val uid = auth.currentUser?.uid ?: error("Bạn cần đăng nhập")
        col(uid).document(id).update("read", true).awaitTask()
    }

    override suspend fun markAllRead(): Result<Unit> = attempt {
        val uid = auth.currentUser?.uid ?: error("Bạn cần đăng nhập")
        val unread = col(uid).whereEqualTo("read", false).get().awaitTask()
        if (!unread.isEmpty) {
            val batch = db.batch()
            unread.documents.forEach { batch.update(it.reference, "read", true) }
            batch.commit().awaitTask()
        }
    }
}