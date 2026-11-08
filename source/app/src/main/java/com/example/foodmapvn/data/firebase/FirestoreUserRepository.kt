package com.example.foodmapvn.data.firebase

import com.example.foodmapvn.data.firebase.mapper.time
import com.example.foodmapvn.data.firebase.mapper.toRegionBadge
import com.example.foodmapvn.data.firebase.mapper.toReviewSummary
import com.example.foodmapvn.data.firebase.mapper.toUserProfile
import com.example.foodmapvn.data.model.CheckedInPlace
import com.example.foodmapvn.data.model.RegionBadge
import com.example.foodmapvn.data.model.RegionCheckIns
import com.example.foodmapvn.domain.BusinessRules
import com.example.foodmapvn.data.model.ReviewSummary
import com.example.foodmapvn.data.model.UserProfile
import com.example.foodmapvn.data.repository.UserRepository
import com.example.foodmapvn.domain.BadgeCalculator
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreUserRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val badgeCalculator: BadgeCalculator,
) : UserRepository {

    private fun userDoc(uid: String) = db.collection(FirestorePaths.USERS).document(uid)

    /** Khách không có hồ sơ: flow không phát gì, ProfileViewModel hiển thị màn Khách. */
    override fun getUser(): Flow<UserProfile> = auth.uidFlow().flatMapLatest { uid ->
        if (uid == null) emptyFlow()
        else userDoc(uid).asFlow().map { it.toUserProfile() }
    }

    override fun getRegionBadges(): Flow<List<RegionBadge>> = auth.uidFlow().flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList())
        else userDoc(uid).collection(FirestorePaths.BADGES).asFlow()
            .map { snapshot -> snapshot.documents.map { it.toRegionBadge(badgeCalculator) } }
            .onStart { emit(emptyList()) }
    }

    /** Tên quán đã tra (đỡ đọc lại mỗi lần danh sách check-in thay đổi). */
    private val placeNames = java.util.concurrent.ConcurrentHashMap<String, String>()

    override fun getCheckInsByRegion(): Flow<Map<String, RegionCheckIns>> = auth.uidFlow().flatMapLatest { uid ->
        if (uid == null) flowOf(emptyMap())
        else db.collection(FirestorePaths.CHECKINS).whereEqualTo("userId", uid).asFlow()
            .map { snapshot ->
                data class Row(val placeId: String, val regionId: String, val at: Long)
                val rows = snapshot.documents
                    .filter { it.getBoolean("valid") != false }
                    .mapNotNull { d ->
                        val placeId = d.getString("placeId") ?: return@mapNotNull null
                        val regionId = d.getString("regionId") ?: return@mapNotNull null
                        Row(placeId, regionId, d.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis())
                    }
                // Lấy tên các quán chưa biết (mỗi quán đọc 1 lần)
                rows.map { it.placeId }.distinct().filter { !placeNames.containsKey(it) }.forEach { id ->
                    val name = runCatching {
                        db.collection(FirestorePaths.PLACES).document(id).get().awaitTask().getString("name")
                    }.getOrNull()
                    placeNames[id] = name ?: "Quán đã xoá"
                }
                val rules = BusinessRules()
                rows.groupBy { it.regionId }.mapValues { (regionId, list) ->
                    val sorted = list.sortedBy { it.at }
                    // Thời điểm đạt: lần check-in đầu tiên mà đủ số lần và đủ số quán khác nhau
                    var achievedAt: Long? = null
                    val seen = mutableSetOf<String>()
                    sorted.forEachIndexed { i, r ->
                        seen += r.placeId
                        if (achievedAt == null && i + 1 >= rules.localBadgeCheckIns && seen.size >= rules.localBadgeMinPlaces) {
                            achievedAt = r.at
                        }
                    }
                    RegionCheckIns(
                        regionId = regionId,
                        checkIns = list.size,
                        places = list.groupBy { it.placeId }.map { (pid, items) ->
                            CheckedInPlace(pid, placeNames[pid] ?: pid, items.size, items.maxOf { it.at })
                        }.sortedByDescending { it.lastAtMillis },
                        achievedAtMillis = achievedAt,
                    )
                }
            }
            .onStart { emit(emptyMap()) }
    }

    /** 5 đánh giá gần nhất (màn Hồ sơ). */
    override fun getRecentReviews(): Flow<List<ReviewSummary>> = myReviews(limit = 5)

    override fun getAllReviews(): Flow<List<ReviewSummary>> = myReviews(limit = null)

    /** Lấy đánh giá của người dùng rồi sắp xếp trong app (không cần tạo index trên Firestore). */
    private fun myReviews(limit: Int?): Flow<List<ReviewSummary>> = auth.uidFlow().flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList())
        else db.collection(FirestorePaths.REVIEWS).whereEqualTo("userId", uid).asFlow()
            .map { snapshot ->
                snapshot.documents
                    .sortedByDescending { it.time("createdAt") }
                    .let { if (limit != null) it.take(limit) else it }
                    .map { it.toReviewSummary() }
            }
            .onStart { emit(emptyList()) }
    }

    override suspend fun updateProfile(name: String, avatarUrl: String?): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("Bạn cần đăng nhập để sửa hồ sơ")
        val updates = mutableMapOf<String, Any>("name" to name.trim())
        avatarUrl?.let { updates["avatarUrl"] = it }
        userDoc(user.uid).update(updates).awaitTask()
        // Cập nhật luôn hồ sơ của Firebase Auth cho khớp (tên/ảnh hiển thị ở chỗ khác)
        val request = UserProfileChangeRequest.Builder().setDisplayName(name.trim())
        avatarUrl?.let { request.setPhotoUri(Uri.parse(it)) }
        user.updateProfile(request.build()).awaitTask()
        Unit
    }.recoverCatching { e ->
        val message = if ("PERMISSION_DENIED" in (e.message ?: ""))
            "Firestore từ chối lưu: hãy Publish lại Rules mới nhất (firebase/firestore.rules) trên Firebase Console"
        else e.message ?: "Không lưu được hồ sơ"
        throw Exception(message, e)
    }

    override fun hasUnreadNotification(): Flow<Boolean> = auth.uidFlow().flatMapLatest { uid ->
        if (uid == null) flowOf(false)
        else userDoc(uid).collection(FirestorePaths.NOTIFICATIONS)
            .whereEqualTo("read", false).limit(1).asFlow()
            .map { !it.isEmpty }
            .onStart { emit(false) }
    }
}
