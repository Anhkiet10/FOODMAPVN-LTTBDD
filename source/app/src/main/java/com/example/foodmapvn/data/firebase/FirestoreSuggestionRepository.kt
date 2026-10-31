package com.example.foodmapvn.data.firebase

import com.example.foodmapvn.data.firebase.mapper.time
import com.example.foodmapvn.data.firebase.mapper.toPlaceSuggestion
import com.example.foodmapvn.data.model.NewSuggestion
import com.example.foodmapvn.data.model.PlaceSuggestion
import com.example.foodmapvn.data.repository.SuggestionRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

// Đề xuất quán lưu
@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreSuggestionRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : SuggestionRepository {

    // Chỉ lọc theo userId, sắp xếp trong app Khách
    override fun getMySuggestions(): Flow<List<PlaceSuggestion>> = auth.uidFlow().flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList())
        else db.collection(FirestorePaths.SUGGESTIONS).whereEqualTo("userId", uid).asFlow()
            .map { snapshot ->
                snapshot.documents
                    // createdAt chưa có (đang chờ server ghi) -> coi là mới nhất
                    .sortedByDescending { it.time("createdAt")?.seconds ?: Long.MAX_VALUE }
                    .mapNotNull { it.toPlaceSuggestion() }
            }
    }

    override suspend fun submit(input: NewSuggestion): Result<Unit> = try {
        val uid = auth.currentUser?.uid ?: error("Bạn cần đăng nhập để đề xuất quán")
        db.collection(FirestorePaths.SUGGESTIONS).document().set(
            mapOf(
                "userId" to uid,
                "regionId" to input.regionId,
                "dishId" to input.dishId,
                "name" to input.name,
                "address" to input.address,
                "location" to GeoPoint(input.location.latitude, input.location.longitude),
                "price" to input.price,
                "openHours" to input.openHours,
                "photoUrl" to input.photoUrl,
                "status" to "pending",
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).awaitTask()
        // Tăng số "Quán đề xuất" ở Hồ sơ
        runCatching {
            db.collection(FirestorePaths.USERS).document(uid)
                .update("stats.suggestions", FieldValue.increment(1)).awaitTask()
        }
        Result.success(Unit)
    } catch (e: Exception) {
        val message = when {
            e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Không gửi được: vùng này chưa có dữ liệu trên hệ thống hoặc tài khoản bị hạn chế"
            e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.UNAVAILABLE ->
                "Không có kết nối mạng"
            else -> e.message ?: "Đã có lỗi xảy ra"
        }
        Result.failure(Exception(message, e))
    }
}