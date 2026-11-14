package com.example.foodmapvn.data.firebase

import com.example.foodmapvn.data.firebase.mapper.toPlaceSuggestion
import com.example.foodmapvn.data.model.NewSuggestion
import com.example.foodmapvn.data.model.PlaceSuggestion
import com.example.foodmapvn.data.repository.SuggestionRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * suggestions/{id}: doanh nghiệp đã được duyệt gửi đề xuất (status luôn "pending").
 * Duyệt / từ chối do Admin làm ở FirestoreAdminRepository.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreSuggestionRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : SuggestionRepository {

    private val col get() = db.collection(FirestorePaths.SUGGESTIONS)

    private fun DocumentSnapshot.stamp(): Timestamp? = try { getTimestamp("createdAt") } catch (e: Exception) { null }

    override fun getMySuggestions(): Flow<List<PlaceSuggestion>> = auth.uidFlow().flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList())
        else col.whereEqualTo("userId", uid).asFlow().map { snap ->
            snap.documents
                .sortedByDescending { it.stamp()?.seconds ?: Long.MAX_VALUE }   // mới nhất trước
                .mapNotNull { it.toPlaceSuggestion() }
        }.catch { emit(emptyList()) }
    }

    override suspend fun submit(input: NewSuggestion): Result<Unit> = try {
        val uid = auth.currentUser?.uid ?: error("Bạn cần đăng nhập để đề xuất quán")
        // Đủ trường bắt buộc của isValidSuggestion (firestore.rules)
        val data = hashMapOf<String, Any>(
            "userId" to uid,
            "regionId" to input.regionId,
            "dishId" to input.dishId,
            "name" to input.name.trim(),
            "address" to input.address.trim(),
            "location" to GeoPoint(input.location.latitude, input.location.longitude),
            "status" to "pending",
            "createdAt" to FieldValue.serverTimestamp(),
        )
        input.price?.let { data["price"] = it }
        input.openHours?.takeIf { it.isNotBlank() }?.let { data["openHours"] = it.trim() }
        input.photoUrl?.takeIf { it.isNotBlank() }?.let { data["photoUrl"] = it }
        col.document().set(data).awaitTask()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val msg = when {
            e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Chỉ tài khoản doanh nghiệp đã được Admin duyệt mới được đề xuất quán"
            e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.UNAVAILABLE ->
                "Không có kết nối mạng"
            else -> e.message ?: "Không gửi được đề xuất"
        }
        Result.failure(Exception(msg, e))
    }
}