package com.example.foodmapvn.data.firebase

import com.example.foodmapvn.data.firebase.mapper.time
import com.example.foodmapvn.data.firebase.mapper.toRegionBadge
import com.example.foodmapvn.data.firebase.mapper.toReviewSummary
import com.example.foodmapvn.data.firebase.mapper.toUserProfile
import com.example.foodmapvn.data.model.RegionBadge
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

    /** Lấy đánh giá của người dùng rồi sắp xếp trong app (không cần tạo index trên Firestore). */
    override fun getRecentReviews(): Flow<List<ReviewSummary>> = auth.uidFlow().flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList())
        else db.collection(FirestorePaths.REVIEWS).whereEqualTo("userId", uid).asFlow()
            .map { snapshot ->
                snapshot.documents
                    .sortedByDescending { it.time("createdAt") }
                    .take(5)
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
