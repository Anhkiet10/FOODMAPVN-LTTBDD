package com.example.foodmapvn.data.firebase

import com.example.foodmapvn.data.firebase.mapper.str
import com.example.foodmapvn.data.model.Business
import com.example.foodmapvn.data.model.BusinessStatus
import com.example.foodmapvn.data.model.BusinessType
import com.example.foodmapvn.data.repository.BusinessRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Locale

/** businesses/{uid}: mỗi tài khoản 1 hồ sơ doanh nghiệp. Rules: firestore.rules > match /businesses. */
@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreBusinessRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : BusinessRepository {

    private fun doc(uid: String) = db.collection(FirestorePaths.BUSINESSES).document(uid)

    override fun getMyBusiness(): Flow<Business?> = auth.uidFlow().flatMapLatest { uid ->
        if (uid == null) flowOf(null)
        else doc(uid).asFlow().map { if (it.exists()) it.toBusiness() else null }
    }

    override fun canSuggestPlace(): Flow<Boolean> = getMyBusiness().map { it?.canSuggestPlace == true }

    override suspend fun register(name: String, type: BusinessType): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Hãy đăng nhập để đăng ký doanh nghiệp")
        BusinessRepository.validateName(name)?.let { error(it) }
        val cleanName = name.trim().replace(Regex("\\s+"), " ")
        val ref = doc(uid)
        val existing = ref.get().awaitTask()
        if (!existing.exists()) {
            ref.set(
                mapOf(
                    "ownerId" to uid,
                    "name" to cleanName,
                    "type" to type.value,
                    "status" to BusinessStatus.PENDING.value,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            ).awaitTask()
        } else {
            check(existing.str("status") != BusinessStatus.APPROVED.value) { "Doanh nghiệp đã được duyệt" }
            // Gửi lại sau khi bị từ chối (hoặc sửa khi đang chờ)
            ref.update(
                mapOf(
                    "name" to cleanName,
                    "type" to type.value,
                    "status" to BusinessStatus.PENDING.value,
                    "rejectReason" to FieldValue.delete(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).awaitTask()
        }
        Unit
    }

    override fun getPendingBusinesses(): Flow<List<Business>> =
        db.collection(FirestorePaths.BUSINESSES)
            .whereEqualTo("status", BusinessStatus.PENDING.value)
            .asFlow()
            .map { snapshot ->
                snapshot.documents
                    .sortedBy { (it.get("createdAt") as? Timestamp)?.seconds ?: Long.MAX_VALUE }
                    .map { it.toBusiness() }
            }

    override suspend fun review(ownerId: String, approve: Boolean, rejectReason: String?): Result<Unit> = runCatching {
        val adminUid = auth.currentUser?.uid ?: error("Hãy đăng nhập")
        require(approve || !rejectReason.isNullOrBlank()) { "Cần nhập lý do từ chối" }
        doc(ownerId).update(
            mapOf(
                "status" to (if (approve) BusinessStatus.APPROVED else BusinessStatus.REJECTED).value,
                "rejectReason" to (if (approve) FieldValue.delete() else rejectReason!!.trim()),
                "reviewedAt" to FieldValue.serverTimestamp(),
                "reviewedBy" to adminUid,
            )
        ).awaitTask()
        Unit
    }

    private fun DocumentSnapshot.toBusiness() = Business(
        ownerId = str("ownerId") ?: id,
        name = str("name") ?: "",
        type = BusinessType.from(str("type")),
        status = BusinessStatus.from(str("status")),
        rejectReason = str("rejectReason"),
        createdAt = (get("createdAt") as? Timestamp)?.toDate()?.let { DATE.format(it) },
    )

    private companion object {
        val DATE = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))
    }
}
