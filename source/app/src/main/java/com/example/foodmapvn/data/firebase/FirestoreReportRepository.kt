package com.example.foodmapvn.data.firebase

import com.example.foodmapvn.data.repository.ReportReason
import com.example.foodmapvn.data.repository.ReportRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException

/** reports/{id}: người dùng đăng nhập gửi báo cáo (status luôn "pending"); chỉ Admin đọc/xử lý. */
class FirestoreReportRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : ReportRepository {

    override suspend fun report(
        targetType: String,
        targetId: String,
        reason: ReportReason,
        note: String,
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(Exception("Bạn cần đăng nhập để gửi báo cáo"))
        return try {
            val data = hashMapOf<String, Any>(
                "reporterId" to uid,
                "targetType" to targetType,          // "PLACE" | "REVIEW"
                "targetId" to targetId,
                "reason" to reason.name,             // WRONG_PRICE | BAD_CONTENT | NOT_EXIST | OTHER
                "status" to "pending",
                "createdAt" to FieldValue.serverTimestamp(),
            )
            if (note.isNotBlank()) data["description"] = note.trim()
            db.collection(FirestorePaths.REPORTS).document().set(data).awaitTask()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val msg = when {
                e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "Không có quyền gửi báo cáo (kiểm tra Rules đã Publish)"
                e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "Không có kết nối mạng"
                else -> e.message ?: "Không gửi được báo cáo"
            }
            Result.failure(Exception(msg, e))
        }
    }
}