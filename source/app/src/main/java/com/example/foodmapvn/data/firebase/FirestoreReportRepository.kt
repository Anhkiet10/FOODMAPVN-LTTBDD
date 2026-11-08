package com.example.foodmapvn.data.firebase

import com.example.foodmapvn.data.repository.ReportReason
import com.example.foodmapvn.data.repository.ReportRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

/** reports/{id}: người dùng chỉ được tạo, Admin đọc và xử lý (firestore.rules > match /reports). */
class FirestoreReportRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : ReportRepository {

    override suspend fun report(targetType: String, targetId: String, reason: ReportReason, note: String): Result<Unit> =
        runCatching {
            val uid = auth.currentUser?.uid ?: error("Hãy đăng nhập để gửi báo cáo")
            // Ghi rõ lý do theo nhãn hiển thị (vd. "Đánh giá giả / spam" dùng mã OTHER) + ghi chú của người báo cáo
            val description = buildString {
                append("[").append(reason.label).append("]")
                if (note.isNotBlank()) append(" ").append(note.trim().take(500))
            }
            db.collection(FirestorePaths.REPORTS).add(
                mapOf(
                    "reporterId" to uid,
                    "targetType" to targetType,
                    "targetId" to targetId,
                    "reason" to reason.code,
                    "description" to description,
                    "status" to "pending",
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            ).awaitTask()
            Unit
        }
}
