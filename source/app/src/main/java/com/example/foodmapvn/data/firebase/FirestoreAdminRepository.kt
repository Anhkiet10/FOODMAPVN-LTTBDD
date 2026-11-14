package com.example.foodmapvn.data.firebase

import com.example.foodmapvn.data.firebase.mapper.toAdminDish
import com.example.foodmapvn.data.firebase.mapper.toAdminRegion
import com.example.foodmapvn.data.firebase.mapper.toAdminUser
import com.example.foodmapvn.data.firebase.mapper.toPlaceSuggestion
import com.example.foodmapvn.data.model.AdminDish
import com.example.foodmapvn.data.model.AdminRegion
import com.example.foodmapvn.data.model.AdminReport
import com.example.foodmapvn.data.model.AdminReportStatus
import com.example.foodmapvn.data.model.AdminStats
import com.example.foodmapvn.data.model.AdminSuggestion
import com.example.foodmapvn.data.model.AdminUser
import com.example.foodmapvn.data.model.SuggestionStatus
import com.example.foodmapvn.data.repository.AdminRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.GeoPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Admin trên Firestore. Mọi lệnh ghi đều phải qua firestore.rules > isAdmin()
 * (tài khoản có users/{uid}.role == "admin"); nếu không sẽ báo PERMISSION_DENIED.
 */
class FirestoreAdminRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : AdminRepository {

    // ---------- tiện ích ----------
    private fun adminUid(): String = auth.currentUser?.uid ?: error("Bạn cần đăng nhập bằng tài khoản Admin")

    private fun DocumentSnapshot.ts(field: String): Timestamp? = try { getTimestamp(field) } catch (e: Exception) { null }

    private fun date(ts: Timestamp?): String? =
        ts?.toDate()?.let { SimpleDateFormat("dd/MM/yyyy", Locale.US).format(it) }

    private suspend fun attempt(block: suspend () -> Unit): Result<Unit> = try {
        block(); Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val msg = when {
            e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Không có quyền: tài khoản chưa có role admin hoặc Rules chưa được Publish bản mới"
            e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.UNAVAILABLE ->
                "Không có kết nối mạng"
            else -> e.message ?: "Đã có lỗi xảy ra"
        }
        Result.failure(Exception(msg, e))
    }

    private suspend fun nameMap(collection: String): Map<String, String> = runCatching {
        db.collection(collection).get().awaitTask().documents.associate { it.id to (it.getString("name") ?: it.id) }
    }.getOrDefault(emptyMap())

    private suspend fun nameOf(collection: String, id: String): String = runCatching {
        db.collection(collection).document(id).get().awaitTask().getString("name")
    }.getOrNull() ?: "—"

    // ---------- UC-20 Tổng quan ----------
    override fun getStats(): Flow<AdminStats> {
        val weekAgo = Timestamp(Date(System.currentTimeMillis() - 7L * 24 * 3600 * 1000))
        val flows = listOf(
            db.collection(FirestorePaths.SUGGESTIONS).whereEqualTo("status", "pending").asFlow(),
            db.collection(FirestorePaths.REPORTS).whereEqualTo("status", "pending").asFlow(),
            db.collection(FirestorePaths.PLACES).whereEqualTo("priceWarning", true).asFlow(),
            db.collection(FirestorePaths.REVIEWS).whereGreaterThan("createdAt", weekAgo).asFlow(),
            db.collection(FirestorePaths.REGIONS).asFlow(),
            db.collection(FirestorePaths.DISHES).asFlow(),
            db.collection(FirestorePaths.USERS).asFlow(),
        )
        return combine(flows) { s ->
            AdminStats(
                pendingSuggestions = s[0].size(), newReports = s[1].size(), warnedPlaces = s[2].size(),
                reviewsThisWeek = s[3].size(), regionCount = s[4].size(), dishCount = s[5].size(),
                userCount = s[6].size(),
            )
        }.catch { emit(AdminStats()) }
    }

    // ---------- UC-17 Duyệt quán ----------
    override fun getSuggestions(status: SuggestionStatus): Flow<List<AdminSuggestion>> =
        db.collection(FirestorePaths.SUGGESTIONS).whereEqualTo("status", status.name.lowercase()).asFlow()
            .map { snap ->
                val dishes = nameMap(FirestorePaths.DISHES)
                val regions = nameMap(FirestorePaths.REGIONS)
                snap.documents
                    .sortedBy { it.ts("createdAt")?.seconds ?: Long.MAX_VALUE }   // cũ nhất trước
                    .mapNotNull { doc ->
                        doc.toPlaceSuggestion()?.let { s ->
                            AdminSuggestion(
                                suggestion = s,
                                dishName = dishes[s.dishId] ?: "—",
                                regionName = regions[s.regionId] ?: "—",
                                proposerName = nameOf(FirestorePaths.USERS, s.userId),
                            )
                        }
                    }
            }.catch { emit(emptyList()) }

    private fun notification(userId: String) =
        db.collection(FirestorePaths.USERS).document(userId).collection(FirestorePaths.NOTIFICATIONS).document()

    override suspend fun approveSuggestion(item: AdminSuggestion): Result<Unit> = attempt {
        val s = item.suggestion
        val admin = adminUid()
        val placeRef = db.collection(FirestorePaths.PLACES).document()
        // Đủ trường bắt buộc của isValidPlace (firestore.rules)
        val place = hashMapOf<String, Any?>(
            "regionId" to s.regionId, "name" to s.name, "address" to s.address,
            "location" to GeoPoint(s.location.latitude, s.location.longitude),
            "status" to "active", "trustScore" to 50, "isVerified" to false, "priceWarning" to false,
            "reviewCount" to 0, "checkInCount" to 0, "dishIds" to listOf(s.dishId),
            "createdAt" to FieldValue.serverTimestamp(), "suggestedBy" to s.userId,
        )
        s.openHours?.split("-")?.map { it.trim() }?.takeIf { it.size == 2 && it.all { t -> t.isNotEmpty() } }
            ?.let { place["openTime"] = it[0]; place["closeTime"] = it[1] }
        s.photoUrl?.let { place["photos"] = listOf(it) }
        s.price?.let { place["menu"] = listOf(mapOf("dishId" to s.dishId, "name" to item.dishName, "price" to it)) }

        db.batch().apply {
            set(placeRef, place)
            update(
                db.collection(FirestorePaths.SUGGESTIONS).document(s.id),
                mapOf(
                    "status" to "approved", "reviewedAt" to FieldValue.serverTimestamp(),
                    "reviewedBy" to admin, "placeId" to placeRef.id,
                ),
            )
            set(
                notification(s.userId),
                mapOf(
                    "type" to "SUGGESTION_APPROVED", "title" to "Đề xuất quán đã được duyệt",
                    "body" to "Quán \"${s.name}\" đã được thêm lên bản đồ.", "read" to false,
                    "createdAt" to FieldValue.serverTimestamp(), "refId" to placeRef.id,
                ),
            )
        }.commit().awaitTask()
        // Tăng bộ đếm quán của vùng (lỗi ở bước phụ này không làm hỏng việc duyệt)
        runCatching { db.collection(FirestorePaths.REGIONS).document(s.regionId).update("placeCount", FieldValue.increment(1)).awaitTask() }
    }

    override suspend fun rejectSuggestion(item: AdminSuggestion, reason: String): Result<Unit> = attempt {
        val why = reason.trim()
        require(why.isNotEmpty()) { "Cần nhập lý do từ chối" }
        val s = item.suggestion
        db.batch().apply {
            update(
                db.collection(FirestorePaths.SUGGESTIONS).document(s.id),
                mapOf(
                    "status" to "rejected", "rejectReason" to why,
                    "reviewedAt" to FieldValue.serverTimestamp(), "reviewedBy" to adminUid(),
                ),
            )
            set(
                notification(s.userId),
                mapOf(
                    "type" to "SUGGESTION_REJECTED", "title" to "Đề xuất quán bị từ chối",
                    "body" to "Quán \"${s.name}\": $why", "read" to false,
                    "createdAt" to FieldValue.serverTimestamp(), "refId" to s.id,
                ),
            )
        }.commit().awaitTask()
    }

    // ---------- UC-18 Báo cáo ----------
    override fun getReports(status: AdminReportStatus): Flow<List<AdminReport>> =
        db.collection(FirestorePaths.REPORTS).whereEqualTo("status", status.code).asFlow()
            .map { snap ->
                snap.documents
                    .sortedByDescending { it.ts("createdAt")?.seconds ?: Long.MAX_VALUE }
                    .map { toReport(it) }
            }.catch { emit(emptyList()) }

    private suspend fun toReport(d: DocumentSnapshot): AdminReport {
        val type = d.getString("targetType") ?: "PLACE"
        val target = d.getString("targetId") ?: ""
        var title = "—"
        var quote = d.getString("description")
        if (type == "REVIEW") {
            runCatching { db.collection(FirestorePaths.REVIEWS).document(target).get().awaitTask() }.getOrNull()?.let { r ->
                title = r.getString("placeName") ?: "—"
                r.getString("content")?.takeIf { it.isNotBlank() }?.let { quote = it }
            }
        } else {
            title = nameOf(FirestorePaths.PLACES, target)
        }
        return AdminReport(
            id = d.id, targetType = type, targetId = target, title = title, quote = quote,
            reasonCode = d.getString("reason") ?: "OTHER",
            reporterName = nameOf(FirestorePaths.USERS, d.getString("reporterId") ?: ""),
            status = AdminReportStatus.from(d.getString("status")),
            createdAt = date(d.ts("createdAt")),
        )
    }

    private fun closeReport(batch: com.google.firebase.firestore.WriteBatch, report: AdminReport, status: String, result: String) {
        batch.update(
            db.collection(FirestorePaths.REPORTS).document(report.id),
            mapOf("status" to status, "handledBy" to adminUid(), "result" to result),
        )
    }

    override suspend fun dismissReport(report: AdminReport): Result<Unit> = attempt {
        db.batch().also { closeReport(it, report, "dismissed", "Bỏ qua") }.commit().awaitTask()
    }

    override suspend fun hideReportedReview(report: AdminReport): Result<Unit> = attempt {
        db.batch().also {
            it.update(db.collection(FirestorePaths.REVIEWS).document(report.targetId), "status", "hidden")
            closeReport(it, report, "resolved", "Đã ẩn đánh giá")
        }.commit().awaitTask()
    }

    override suspend fun markPlaceClosed(report: AdminReport): Result<Unit> = attempt {
        db.batch().also {
            it.update(db.collection(FirestorePaths.PLACES).document(report.targetId), "status", "closed")
            closeReport(it, report, "resolved", "Đã đánh dấu đóng cửa")
        }.commit().awaitTask()
    }

    override suspend fun resolveReport(report: AdminReport): Result<Unit> = attempt {
        db.batch().also { closeReport(it, report, "resolved", "Đã xử lý") }.commit().awaitTask()
    }

    // ---------- UC-19 Đặc sản & vùng ----------
    override fun getRegions(): Flow<List<AdminRegion>> =
        db.collection(FirestorePaths.REGIONS).asFlow().map { snap ->
            snap.documents.map { it.toAdminRegion() }.sortedBy { it.name }
        }.catch { emit(emptyList()) }

    override fun getDishes(regionId: String): Flow<List<AdminDish>> =
        db.collection(FirestorePaths.DISHES).whereEqualTo("regionId", regionId).asFlow().map { snap ->
            snap.documents.map { it.toAdminDish() }.sortedBy { it.name }
        }.catch { emit(emptyList()) }

    override suspend fun setDishVisible(dishId: String, visible: Boolean): Result<Unit> = attempt {
        db.collection(FirestorePaths.DISHES).document(dishId)
            .update("status", if (visible) "active" else "hidden").awaitTask()
    }

    override suspend fun addDish(regionId: String, name: String, unit: String): Result<Unit> = attempt {
        val n = name.trim()
        require(n.isNotEmpty()) { "Cần nhập tên món" }
        db.collection(FirestorePaths.DISHES).document().set(
            mapOf(
                "regionId" to regionId, "name" to n, "unit" to unit,
                "verifiedPlaceCount" to 0, "status" to "active",
            ),
        ).awaitTask()
        runCatching { db.collection(FirestorePaths.REGIONS).document(regionId).update("dishCount", FieldValue.increment(1)).awaitTask() }
    }

    // ---------- Người dùng ----------
    override fun getUsers(): Flow<List<AdminUser>> =
        db.collection(FirestorePaths.USERS).asFlow().map { snap ->
            snap.documents.map { it.toAdminUser() }.sortedBy { it.name }
        }.catch { emit(emptyList()) }

    override suspend fun setUserLocked(uid: String, locked: Boolean): Result<Unit> = attempt {
        db.collection(FirestorePaths.USERS).document(uid)
            .update("status", if (locked) "locked" else "active").awaitTask()
    }
}